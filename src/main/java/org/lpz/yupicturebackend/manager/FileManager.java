package org.lpz.yupicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicturebackend.config.CosClientConfig;
import org.lpz.yupicturebackend.exception.BusinessException;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.lpz.yupicturebackend.model.dto.file.UploadPictureResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * 文件服务
 * @deprecated 已废弃，改为使用 upload 包的模板方法优化
 */
@Deprecated
@Service
@Slf4j
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String filepathPrefix) {

        // 校验图片
        validPicture(multipartFile);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String filename = multipartFile.getOriginalFilename();
        String fileSuffix = FileUtil.getSuffix(filename);
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid,fileSuffix);

        String uploadFilepath = String.format("/%s/%s",filepathPrefix,uploadFileName);
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadFilepath,null);
            multipartFile.transferTo(file);
            // 上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilepath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结构
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight,2).doubleValue();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadFilepath);
            uploadPictureResult.setPicName(FileUtil.mainName(multipartFile.getOriginalFilename()));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            return uploadPictureResult;

        } catch (IOException e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(file);
        }
    }

    /**
     * 校验图片
     * @param multipartFile
     */
    public void validPicture(MultipartFile multipartFile) {

        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR,"文件不能为空");
        // 1.校验图片大小
        long fileSize = multipartFile.getSize();
        final long MAX_SIZE = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 10 * MAX_SIZE,ErrorCode.PARAMS_ERROR,"文件大小不能超过10MB");
        // 2.校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg","png","jpeg","webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),ErrorCode.PARAMS_ERROR,"文件类型错误");

    }


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
