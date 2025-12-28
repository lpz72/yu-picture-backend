package org.lpz.yupicture.interfaces.assembler;

import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.interfaces.dto.user.UserAddRequest;
import org.lpz.yupicture.interfaces.dto.user.UserUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 用户对象转换类
 */
public class UserAssembler {

    public static User toUserEntity(UserAddRequest userAddRequest) {
        User user = new User();
        BeanUtils.copyProperties(userAddRequest,user);
        return user;
    }

    public static User toUserEntity(UserUpdateRequest userUpdateRequest) {
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest,user);
        return user;
    }

}
