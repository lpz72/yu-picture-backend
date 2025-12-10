package org.lpz.yupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import org.lpz.yupicturebackend.exception.BusinessException;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * 通过URL上传图片
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate{
    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        String url = (String) inputSource;
        HttpUtil.downloadFile(url,file);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.getName(fileUrl);
    }

    @Override
    protected void validPicture(Object inputSource) {

        String fileUrl = (String) inputSource;

        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR,"URL地址不能为空");

        try {
            // 1. 验证URL格式
            new URL(fileUrl); // 检查URL格式是否合法
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件地址格式错误");
        }

        // 2. 校验URL协议
        ThrowUtils.throwIf(!fileUrl.startsWith("https://") && !fileUrl.startsWith("http://"),
                ErrorCode.PARAMS_ERROR,"仅支持 http 或 https 协议的文件地址");

        // 3. 发送HEAD请求以验证文件是否存在
        HttpResponse response = null;
        try {
//            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            response = HttpUtil.createRequest(Method.GET, fileUrl).execute();
            // 为正常返回，无需其他操作
            if (response == null) {
                return;
            }

            // 4. 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                final List<String> ALLOW_FORMAT_LIST = Arrays.asList("image/jpg","image/png","image/jpeg","image/webp");
                ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(contentType),ErrorCode.PARAMS_ERROR,"文件类型错误");
            }
            // 5. 校验图片大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                long contentLength = Long.parseLong(contentLengthStr);
                final long MAX_SIZE = 10 * 1024 * 1024; // 10MB
                ThrowUtils.throwIf(contentLength > MAX_SIZE,ErrorCode.PARAMS_ERROR,"文件大小不能超过10MB");
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
