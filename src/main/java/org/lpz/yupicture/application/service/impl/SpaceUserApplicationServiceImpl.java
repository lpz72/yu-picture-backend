package org.lpz.yupicture.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lpz.yupicture.domain.space.service.SpaceUserDomainService;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.lpz.yupicture.interfaces.assembler.SpaceUserAssembler;
import org.lpz.yupicture.interfaces.dto.space.spaceuser.SpaceUserAddRequest;
import org.lpz.yupicture.interfaces.dto.space.spaceuser.SpaceUserQueryRequest;
import org.lpz.yupicture.domain.space.entity.Space;
import org.lpz.yupicture.domain.space.entity.SpaceUser;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.domain.space.valueobject.SpaceRoleEnum;
import org.lpz.yupicture.interfaces.vo.space.SpaceUserVO;
import org.lpz.yupicture.interfaces.vo.space.SpaceVO;
import org.lpz.yupicture.interfaces.vo.user.UserVO;
import org.lpz.yupicture.application.service.SpaceApplicationService;
import org.lpz.yupicture.application.service.SpaceUserApplicationService;
import org.lpz.yupicture.infrastructure.mapper.SpaceUserMapper;
import org.lpz.yupicture.application.service.UserApplicationService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author lpz
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-12-13 21:37:04
*/
@Service
public class SpaceUserApplicationServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserApplicationService {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private SpaceUserDomainService spaceUserDomainService;


    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {

        return spaceUserDomainService.getQueryWrapper(spaceUserQueryRequest);
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {

        User user = userApplicationService.getLoginUser(request);
        UserVO userVO = userApplicationService.getUserVO(user);
        Long spaceId = spaceUser.getSpaceId();
        Space space = spaceApplicationService.getById(spaceId);
        SpaceVO spaceVO = spaceApplicationService.getSpaceVO(space, request);

        SpaceUserVO spaceUserVO = new SpaceUserVO();
        BeanUtils.copyProperties(spaceUser,spaceUserVO);
        spaceUserVO.setUser(userVO);
        spaceUserVO.setSpace(spaceVO);

        return spaceUserVO;
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);

        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        String spaceRole = spaceUser.getSpaceRole();

        // 创建时，userId 和 spaceId 不能为空
        if (add) {
            // 校验 spaceId
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR,"空间 id 不合法");
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null,ErrorCode.NOT_FOUND_ERROR);

            // 校验 userId
            ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR,"用户 id 不合法");
            User user = userApplicationService.getUserById(userId);
            ThrowUtils.throwIf(user == null,ErrorCode.NOT_FOUND_ERROR);
        }

        // 校验 spaceRole，数据库默认为 viewer，所以这里不校验
        SpaceRoleEnum enumByValue = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(spaceRole != null && enumByValue == null,ErrorCode.PARAMS_ERROR,"空间角色不存在");

    }

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);

        SpaceUser spaceUser = SpaceUserAssembler.toSpaceUserEntity(spaceUserAddRequest);

        // 校验 spaceUser
        validSpaceUser(spaceUser, true);
        boolean save = this.save(spaceUser);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "添加空间用户关联失败");
        return spaceUser.getId();

    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }

        // 对象列表 -> 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());

        // 获取要查询的用户id
        Set<Long> userIdList = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        // 获取要查询的空间id
        Set<Long> spaceIdList = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());


        // 一次性查完，用户id 对应一个只包含一个元素的list
        Map<Long,List<User>> userIdUserListMap = userApplicationService.listByIds(userIdList)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        Map<Long,List<Space>> spaceIdUserListMap = spaceApplicationService.listByIds(spaceIdList)
                .stream()
                .collect(Collectors.groupingBy(Space::getId));

        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }

            Long spaceId = spaceUserVO.getSpaceId();
            Space space = new Space();
            if (spaceIdUserListMap.containsKey(spaceId)) {
                space = spaceIdUserListMap.get(spaceId).get(0);
            }

            spaceUserVO.setUser(userApplicationService.getUserVO(user));
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });

        return spaceUserVOList;
    }
}




