package org.lpz.yupicturebackend.common;

import lombok.Data;
import org.lpz.yupicturebackend.exception.ErrorCode;

import java.io.Serializable;

/**
 * 通用返回类
 * @param <T>
 */
@Data
public class Baseresponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public Baseresponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public Baseresponse(int code,T data) {
        this(code,data,"");
    }

    public Baseresponse(ErrorCode errorCode) {
        this(errorCode.getCode(),null,errorCode.getMessage());
    }
}
