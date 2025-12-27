package org.lpz.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.lpz.yupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import org.lpz.yupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import org.lpz.yupicturebackend.model.entity.Picture;
import org.lpz.yupicturebackend.model.entity.Space;
import org.lpz.yupicturebackend.model.entity.SpaceUser;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.enums.SpaceRoleEnum;
import org.lpz.yupicturebackend.model.vo.PictureVO;
import org.lpz.yupicturebackend.model.vo.SpaceUserVO;
import org.lpz.yupicturebackend.model.vo.SpaceVO;
import org.lpz.yupicturebackend.model.vo.UserVO;
import org.lpz.yupicturebackend.service.SpaceService;
import org.lpz.yupicturebackend.service.SpaceUserService;
import org.lpz.yupicturebackend.mapper.SpaceUserMapper;
import org.lpz.yupicturebackend.service.UserService;
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
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;


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

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {

        User user = userService.getLoginUser(request);
        UserVO userVO = userService.getUserVO(user);
        Long spaceId = spaceUser.getSpaceId();
        Space space = spaceService.getById(spaceId);
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);

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
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null,ErrorCode.NOT_FOUND_ERROR);

            // 校验 userId
            ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR,"用户 id 不合法");
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null,ErrorCode.NOT_FOUND_ERROR);
        }

        // 校验 spaceRole，数据库默认为 viewer，所以这里不校验
        SpaceRoleEnum enumByValue = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(spaceRole != null && enumByValue == null,ErrorCode.PARAMS_ERROR,"空间角色不存在");

    }

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);

        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);

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
        Map<Long,List<User>> userIdUserListMap = userService.listByIds(userIdList)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        Map<Long,List<Space>> spaceIdUserListMap = spaceService.listByIds(spaceIdList)
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

            spaceUserVO.setUser(userService.getUserVO(user));
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });

        return spaceUserVOList;
    }
}




