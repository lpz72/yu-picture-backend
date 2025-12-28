package org.lpz.yupicture.interfaces.assembler;

import org.lpz.yupicture.domain.space.entity.Space;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.interfaces.dto.space.SpaceAddRequest;
import org.lpz.yupicture.interfaces.dto.space.SpaceEditRequest;
import org.lpz.yupicture.interfaces.dto.space.SpaceUpdateRequest;
import org.lpz.yupicture.interfaces.dto.user.UserAddRequest;
import org.lpz.yupicture.interfaces.dto.user.UserUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 空间对象转换类
 */
public class SpaceAssembler {

    public static Space toSpaceEntity(SpaceAddRequest spaceAddRequest) {
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest,space);
        return space;
    }

    public static Space toSpaceEntity(SpaceUpdateRequest spaceUpdateRequest) {
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest,space);
        return space;
    }

    public static Space toSpaceEntity(SpaceEditRequest spaceEditRequest) {
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest,space);
        return space;
    }

}
