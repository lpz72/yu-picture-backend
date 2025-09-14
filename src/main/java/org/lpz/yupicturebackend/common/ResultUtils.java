package org.lpz.yupicturebackend.common;

import org.lpz.yupicturebackend.exception.ErrorCode;

public class ResultUtils {

    /**
     * 成功
     * @param data
     * @return
     * @param <T>
     */
    public static <T> Baseresponse<T> success(T data){
        return new Baseresponse<>(0,data,"ok");
    }

    /**
     * 失败
     * @param errorCode
     * @return
     * @param <T>
     */
    public static <T> Baseresponse<T> error(ErrorCode errorCode){
        return new Baseresponse<>(errorCode);
    }

    /**
     * 失败
     * @param code
     * @param message
     * @return
     * @param <T>
     */
    public static <T> Baseresponse<T> error(int code,String message){
        return new Baseresponse<>(code,null,message);
    }

    public static <T> Baseresponse<T> error(ErrorCode errorCode,String message){
        return new Baseresponse<>(errorCode.getCode(),null,message);
    }
}
