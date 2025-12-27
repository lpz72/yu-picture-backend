package org.lpz.yupicturebackend.manager.websocket.model;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 图片编辑消息类型枚举
 */
@Getter
public enum PictureEditMessageTypeEnum {

    INFO("发送通知", "INFO"),
    ERROR("发送错误", "ERROR"),
    ENTER_EDIT("进入编辑状态", "ENTER_EDIT"),
    EXIT_EDIT("退出编辑状态", "EXIT_EDIT"),
    EDIT_ACTION("执行编辑操作", "EDIT_ACTION");

    private final String text;

    private final String value;


    /**
     * @param text  文本
     * @param value 值
     */
    PictureEditMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;

    }

    /**
     * 根据 value 获取枚举
     */
    public static PictureEditMessageTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PictureEditMessageTypeEnum spaceLevelEnum : PictureEditMessageTypeEnum.values()) {
            if (spaceLevelEnum.value.equals(value)) {
                return spaceLevelEnum;
            }
        }
        return null;
    }

}
