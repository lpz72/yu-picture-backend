package org.lpz.yupicture.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lpz.yupicture.domain.space.service.SpaceDomainService;
import org.lpz.yupicture.infrastructure.exception.BusinessException;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.lpz.yupicture.interfaces.dto.space.SpaceAddRequest;
import org.lpz.yupicture.interfaces.dto.space.SpaceQueryRequest;
import org.lpz.yupicture.domain.space.entity.Space;
import org.lpz.yupicture.domain.space.entity.SpaceUser;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.domain.space.valueobject.SpaceLevelEnum;
import org.lpz.yupicture.domain.space.valueobject.SpaceTypeEnum;
import org.lpz.yupicture.interfaces.vo.space.SpaceVO;
import org.lpz.yupicture.interfaces.vo.user.UserVO;
import org.lpz.yupicture.application.service.SpaceApplicationService;
import org.lpz.yupicture.infrastructure.mapper.SpaceMapper;
import org.lpz.yupicture.application.service.SpaceUserApplicationService;
import org.lpz.yupicture.application.service.UserApplicationService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceApplicationService {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceDomainService spaceDomainService;

    @Resource
    @Lazy
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private TransactionTemplate transactionTemplate;

    
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {

        User user = userApplicationService.getLoginUser(request);
        UserVO userVO = userApplicationService.getUserVO(user);
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
        Map<Long,List<User>> userIdUserListMap = userApplicationService.listByIds(userIdList)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }

            spaceVO.setUser(userApplicationService.getUserVO(user));
        });

        spaceVOPage.setRecords(spaceVOList);

        return spaceVOPage;
    }


    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
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
        space.validSpace(true);
        Long userId = user.getId();
        space.setUserId(userId);
        // 校验权限，非管理员只能创建普通级别的空间
        if (!user.isAdmin() && SpaceLevelEnum.COMMON.getValue() != spaceLevel) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限创建该级别空间");
        }
        // 控制同一用户只能创建一个私有空间
        // 针对用户加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long spaceId = transactionTemplate.execute(status -> {
                // 判读是否已创建过
                boolean b = this.lambdaQuery().eq(Space::getUserId, userId).eq(Space::getSpaceType,space.getSpaceType()).exists();
                if (b && !user.isAdmin()) {
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
                    save = spaceUserApplicationService.save(spaceUser);
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




