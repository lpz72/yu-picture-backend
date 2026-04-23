package org.lpz.yupicturebackend.service.impl;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import org.junit.jupiter.api.Test;
import org.lpz.yupicturebackend.service.PictureService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@ActiveProfiles("local")
class PictureServiceImplTest {

    @Resource
    private PictureService pictureService;

    @Test
    void generatePictureInformation() throws NoApiKeyException, UploadFileException {
        List<String> strings = pictureService.generatePictureInformation("https://yu-picture-1319946593.cos.ap-beijing.myqcloud.com/public/1982019081175306241/2026-04-22_lyjeRq7E6zmiz0Db.webp");
        System.out.println(strings.get(0));
        System.out.println(strings.get(1));
    }
}