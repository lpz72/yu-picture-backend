package org.lpz.yupicturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicturebackend.config.CosClientConfig;
import org.lpz.yupicturebackend.exception.BusinessException;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.lpz.yupicturebackend.manager.CosManager;
import org.lpz.yupicturebackend.model.dto.file.UploadPictureResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 图片上传模板类
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    public UploadPictureResult uploadPicture(Object inputSource,String filepathPrefix) {

        // 1. 校验图片
        validPicture(inputSource);
        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String filename = getOriginalFilename(inputSource);
        // AI扩图后，返回的地址会在格式(如jpg)后面加上?和一串参数
        if (filename.contains("?")){
            filename = filename.substring(0, filename.indexOf("?"));
        }
        String fileSuffix = FileUtil.getSuffix(filename);
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid,fileSuffix);

        String uploadFilepath = String.format("/%s/%s",filepathPrefix,uploadFileName);
        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadFilepath,null);
            // 处理文件来源（本地或URL）
            processFile(inputSource,file);
            // 4. 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilepath, file);
            // 获取图片主色调
            String color = cosManager.getImageAve(uploadFilepath);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            imageInfo.setAve(color);
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (ObjUtil.isNotEmpty(objectList)) {
                // 获取压缩后的图片对象信息
                CIObject compressCiObject = objectList.get(0);
                // 缩略图默认为压缩图
                CIObject thumbnailCiObject = compressCiObject;
                // 判断是否存在缩略图
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装返回结果
                return buildResult(imageInfo, filename, compressCiObject, thumbnailCiObject);
            }

            // 5. 封装返回结果
            return buildResult(imageInfo, uploadFilepath, filename, file);

        } catch (IOException e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(file);
        }
    }

    private UploadPictureResult buildResult(ImageInfo imageInfo, String fileName, CIObject compressCiObject, CIObject thumbnailCiObject) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressCiObject.getWidth();
        int picHeight = compressCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight,2).doubleValue();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressCiObject.getKey());
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(fileName));
        uploadPictureResult.setPicSize(compressCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressCiObject.getFormat());
        // 获取图片主色调
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    private UploadPictureResult buildResult(ImageInfo imageInfo, String uploadFilepath, String filename, File file) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight,2).doubleValue();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadFilepath);
        uploadPictureResult.setPicName(FileUtil.mainName(filename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        // 获取图片主色调
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    protected abstract void processFile(Object inputSource, File file) throws IOException;

    /**
     * 获取原始文件名
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 校验图片
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);


    /**
     * 删除临时文件
     * @param file
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }

        // 删除临时文件
        boolean delete = file.delete();
        if (!delete) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
