package org.lpz.yupicturebackend.exception;

import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicturebackend.common.Baseresponse;
import org.lpz.yupicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandle {

    @ExceptionHandler(BusinessException.class)
    public Baseresponse<?> businessExceptionHandle(BusinessException e){
        log.error("BusinessException: ",e);
        return ResultUtils.error(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Baseresponse<?> runtimeExceptionHandle(RuntimeException e){
        log.error("RuntimeException: ",e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"系统错误");
    }

}
