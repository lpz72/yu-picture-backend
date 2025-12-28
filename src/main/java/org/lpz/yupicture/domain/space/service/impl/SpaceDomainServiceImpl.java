package org.lpz.yupicture.domain.space.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lpz.yupicture.application.service.SpaceApplicationService;
import org.lpz.yupicture.application.service.SpaceUserApplicationService;
import org.lpz.yupicture.application.service.UserApplicationService;
import org.lpz.yupicture.domain.space.entity.Space;
import org.lpz.yupicture.domain.space.entity.SpaceUser;
import org.lpz.yupicture.domain.space.service.SpaceDomainService;
import org.lpz.yupicture.domain.space.valueobject.SpaceLevelEnum;
import org.lpz.yupicture.domain.space.valueobject.SpaceTypeEnum;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.infrastructure.exception.BusinessException;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.lpz.yupicture.infrastructure.mapper.SpaceMapper;
import org.lpz.yupicture.interfaces.dto.space.SpaceAddRequest;
import org.lpz.yupicture.interfaces.dto.space.SpaceQueryRequest;
import org.lpz.yupicture.interfaces.vo.space.SpaceVO;
import org.lpz.yupicture.interfaces.vo.user.UserVO;
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
public class SpaceDomainServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceDomainService {


    
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

}




