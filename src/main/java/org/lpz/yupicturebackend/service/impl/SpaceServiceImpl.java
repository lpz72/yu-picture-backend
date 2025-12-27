package org.lpz.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lpz.yupicturebackend.exception.BusinessException;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.lpz.yupicturebackend.manager.sharding.DynamicShardingManager;
import org.lpz.yupicturebackend.model.dto.space.SpaceAddRequest;
import org.lpz.yupicturebackend.model.dto.space.SpaceQueryRequest;
import org.lpz.yupicturebackend.model.entity.Picture;
import org.lpz.yupicturebackend.model.entity.Space;
import org.lpz.yupicturebackend.model.entity.SpaceUser;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.enums.SpaceLevelEnum;
import org.lpz.yupicturebackend.model.enums.SpaceTypeEnum;
import org.lpz.yupicturebackend.model.vo.PictureVO;
import org.lpz.yupicturebackend.model.vo.SpaceVO;
import org.lpz.yupicturebackend.model.vo.UserVO;
import org.lpz.yupicturebackend.service.SpaceService;
import org.lpz.yupicturebackend.mapper.SpaceMapper;
import org.lpz.yupicturebackend.service.SpaceUserService;
import org.lpz.yupicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author lpz
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-11-16 16:57:24
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private SpaceUserService spaceUserService;

    @Resource
    private TransactionTemplate transactionTemplate;

//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;
    
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {

        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }

        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Long userId = spaceQueryRequest.getUserId();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();


        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotNull(spaceLevel),"spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotNull(spaceType),"spaceType", spaceType);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);

        // 排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);


        return queryWrapper;
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {

        User user = userService.getLoginUser(request);
        UserVO userVO = userService.getUserVO(user);
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
        spaceVO.setUser(userVO);

        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaces = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getPages(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaces)) {
            return spaceVOPage;
        }

        // 对象列表 -> 封装对象列表
        List<SpaceVO> spaceVOList = spaces.stream().map(SpaceVO::objToVo).collect(Collectors.toList());

        // 获取要查询的用户id
        Set<Long> userIdList = spaces.stream().map(Space::getUserId).collect(Collectors.toSet());

        // 一次性查完，用户id 对应一个只包含一个元素的list
        Map<Long,List<User>> userIdUserListMap = userService.listByIds(userIdList)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }

            spaceVO.setUser(userService.getUserVO(user));
        });

        spaceVOPage.setRecords(spaceVOList);

        return spaceVOPage;
    }

    @Override
    public void validSpace(Space space,boolean add) {

        ThrowUtils.throwIf(space == null,ErrorCode.PARAMS_ERROR);

        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

        // 如果是创建空间
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间级别不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间类型不能为空");
            }

        }

        if (spaceLevel != null && enumByValue == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间级别不存在");
        }


        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称过长");
        }

        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间类型不存在");
        }


    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {

        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        Long maxSize = space.getMaxSize();
        Long maxCount = space.getMaxCount();
        if (enumByValue != null) {

            // 如果没有传最大大小，则使用枚举默认值
            if (maxSize == null) {
                space.setMaxSize(enumByValue.getMaxSize());
            }

            if (maxCount == null) {
                space.setMaxCount(enumByValue.getMaxCount());
            }
        }

    }

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User user) {
        ThrowUtils.throwIf(spaceAddRequest == null || user == null, ErrorCode.PARAMS_ERROR);

        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest,space);
        // 如果空间类型为空，则默认为私有空间
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }

        Integer spaceLevel = space.getSpaceLevel();

        // 填充参数默认值
        String spaceName = space.getSpaceName();
        if (StrUtil.isBlank(spaceName)) {
            space.setSpaceName("默认空间");
        }
        if (spaceLevel == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }

        // 填充数据
        fillSpaceBySpaceLevel(space);

        // 校验参数
        validSpace(space,true);
        Long userId = user.getId();
        space.setUserId(userId);
        // 校验权限，非管理员只能创建普通级别的空间
        if (!userService.isAdmin(user) && SpaceLevelEnum.COMMON.getValue() != spaceLevel) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限创建该级别空间");
        }
        // 控制同一用户只能创建一个私有空间
        // 针对用户加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long spaceId = transactionTemplate.execute(status -> {
                // 判读是否已创建过
                boolean b = this.lambdaQuery().eq(Space::getUserId, userId).eq(Space::getSpaceType,space.getSpaceType()).exists();
                if (b && !userService.isAdmin(user)) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR,"每类空间每个用户仅能创建一个");
                }
                // 写入数据库
                boolean save = this.save(space);
                ThrowUtils.throwIf(!save,ErrorCode.OPERATION_ERROR);
                // 如果是团队空间，则创建空间用户关联
                if (SpaceTypeEnum.TEAM.getValue().equals(space.getSpaceType())) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole("admin");
                    save = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!save,ErrorCode.OPERATION_ERROR,"创建团队成员记录失败");
                }

                // 创建分表
//                dynamicShardingManager.createSpaceTable(space);
                // 返回空间 id
                return space.getId();
            });
            return spaceId;
        }
    }
}




