package org.lpz.yupicturebackend.manager.websocket.model;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 图片编辑操作类型枚举
 */
@Getter
public enum PictureEditActionTypeEnum {

    ZOOM_IN("放大操作", "ZOOM_IN"),
    ZOOM_OUT("缩小操作", "ZOOM_OUT"),
    ROTATE_LEFT("左旋操作", "ROTATE_LEFT"),
    ROTATE_RIGHT("右旋操作", "ROTATE_RIGHT");

    private final String text;

    private final String value;


    /**
     * @param text  文本
     * @param value 值
     */
    PictureEditActionTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;

    }

    /**
     * 根据 value 获取枚举
     */
    public static PictureEditActionTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PictureEditActionTypeEnum spaceLevelEnum : PictureEditActionTypeEnum.values()) {
            if (spaceLevelEnum.value.equals(value)) {
                return spaceLevelEnum;
            }
        }
        return null;
    }

}
