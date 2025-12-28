package org.lpz.yupicture.domain.space.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lpz.yupicture.application.service.SpaceApplicationService;
import org.lpz.yupicture.application.service.SpaceUserApplicationService;
import org.lpz.yupicture.application.service.UserApplicationService;
import org.lpz.yupicture.domain.space.entity.Space;
import org.lpz.yupicture.domain.space.entity.SpaceUser;
import org.lpz.yupicture.domain.space.service.SpaceUserDomainService;
import org.lpz.yupicture.domain.space.valueobject.SpaceRoleEnum;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.lpz.yupicture.infrastructure.mapper.SpaceUserMapper;
import org.lpz.yupicture.interfaces.dto.space.spaceuser.SpaceUserAddRequest;
import org.lpz.yupicture.interfaces.dto.space.spaceuser.SpaceUserQueryRequest;
import org.lpz.yupicture.interfaces.vo.space.SpaceUserVO;
import org.lpz.yupicture.interfaces.vo.space.SpaceVO;
import org.lpz.yupicture.interfaces.vo.user.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author lpz
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-12-13 21:37:04
*/
@Service
public class SpaceUserDomainServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserDomainService {


    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {

        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }

        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();

        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotNull(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        queryWrapper.eq(StrUtil.isNotBlank(spaceRole), "spaceRole", spaceRole);


        return queryWrapper;
    }


}




