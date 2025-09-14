package org.lpz.yupicturebackend.controller;

import org.lpz.yupicturebackend.common.Baseresponse;
import org.lpz.yupicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查
     * @return
     */
    @GetMapping("/health")
    public Baseresponse<String> health() {
        return ResultUtils.success("ok");
    }
}
