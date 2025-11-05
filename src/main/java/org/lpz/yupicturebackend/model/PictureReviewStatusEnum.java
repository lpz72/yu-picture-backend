package org.lpz.yupicturebackend.model;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 图片审核状态枚举
 */
@Getter
public enum PictureReviewStatusEnum {

    REVIEWING("待审核",0),
    PASS("通过",1),
    REJECT("拒绝",2);

    private final String name;
    private final int value;

    PictureReviewStatusEnum(String name, int value){
        this.name = name;
        this.value = value;
    }


    /**
     * 根据value获取枚举值
     * @param value
     * @return
     */
    public static PictureReviewStatusEnum getEnumByValue(int value){
        if(ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if (pictureReviewStatusEnum.getValue() == value) {
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }

}
