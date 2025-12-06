package org.lpz.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {

    USER("用户","user"),
    ADMIN("管理员","admin");

    private final String name;
    private final String value;

    UserRoleEnum(String name,String value){
        this.name = name;
        this.value = value;
    }


    /**
     * 根据value获取枚举值
     * @param value
     * @return
     */
    public static UserRoleEnum getEnumByValue(String value){
        if(ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum userRoleEnum:UserRoleEnum.values()) {
            if (userRoleEnum.getValue().equals(value)) {
                return userRoleEnum;
            }
        }
        return null;
    }

}
