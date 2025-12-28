package org.lpz.yupicture.domain.space.valueobject;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum SpaceTypeEnum {

    PRIVATE("私有空间", 0),
    TEAM("团队空间", 1);

    private final String text;
    private final Integer value;

    SpaceTypeEnum(String name, Integer value) {
        this.text = name;
        this.value = value;
    }


    /**
     * 根据value获取枚举值
     *
     * @param value
     * @return
     */
    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceTypeEnum userRoleEnum : SpaceTypeEnum.values()) {
            if (userRoleEnum.getValue().equals(value)) {
                return userRoleEnum;
            }
        }
        return null;
    }

    /**
     * 获取所有枚举的文本列表
     *
     * @return 文本列表
     */
    public static List<String> getAllTexts() {
        return Arrays.stream(SpaceTypeEnum.values())
                .map(SpaceTypeEnum::getText)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有枚举的值列表
     *
     * @return 值列表
     */
    public static List<Integer> getAllValues() {
        return Arrays.stream(SpaceTypeEnum.values())
                .map(SpaceTypeEnum::getValue)
                .collect(Collectors.toList());
    }

}
