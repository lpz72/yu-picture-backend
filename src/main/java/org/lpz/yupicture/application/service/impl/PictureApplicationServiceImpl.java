package org.lpz.yupicture.application.service.impl;
import java.util.*;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicture.domain.picture.service.PictureDomainService;
import org.lpz.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.lpz.yupicture.interfaces.dto.picture.*;
import org.lpz.yupicture.domain.picture.entity.Picture;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.interfaces.vo.picture.PictureVO;
import org.lpz.yupicture.application.service.PictureApplicationService;
import org.lpz.yupicture.infrastructure.mapper.PictureMapper;
import org.lpz.yupicture.application.service.UserApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author lpz
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-10-25 17:43:32
*/
@Service
@Slf4j
public class PictureApplicationServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureApplicationService {

    @Autowired
    private UserApplicationService userApplicationService;

    @Resource
    private PictureDomainService pictureDomainService;

    private final Set<String> keys = new ConcurrentHashSet<>();


    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(inputSource == null || request == null,ErrorCode.PARAMS_ERROR,"请求参数为空");

        User loginUser = userApplicationService.getLoginUser(request);

        return pictureDomainService.uploadPicture(inputSource, pictureUploadRequest,loginUser);
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        return pictureDomainService.getQueryWrapper(pictureQueryRequest);
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {

        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userApplicationService.getUserById(userId);
            pictureVO.setUser(userApplicationService.getUserVO(user));
        }

        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {

        List<Picture> pictures = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getPages(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictures)) {
            return pictureVOPage;
        }

        // 对象列表 -> 封装对象列表
        List<PictureVO> pictureVOList = pictures.stream().map(PictureVO::objToVo).collect(Collectors.toList());

        // 获取要查询的用户id
        Set<Long> userIdList = pictures.stream().map(Picture::getUserId).collect(Collectors.toSet());

        // 一次性查完，用户id 对应一个只包含一个元素的list
        Map<Long,List<User>> userIdUserListMap = userApplicationService.listByIds(userIdList)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }

            pictureVO.setUser(userApplicationService.getUserVO(user));
        });

        pictureVOPage.setRecords(pictureVOList);

        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        picture.validPicture();
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        pictureDomainService.doPictureReview(pictureReviewRequest,loginUser);
    }

    @Override
    public void fillReviewParams(Picture picture, User user) {
        pictureDomainService.fillReviewParams(picture,user);
    }

    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        return pictureDomainService.uploadPictureByBatch(pictureUploadByBatchRequest,loginUser);
    }

    @Override
    public void addCacheKey(String key) {
        keys.add(key);
    }

    @Override
    public void deleteCacheKeys(StringRedisTemplate stringRedisTemplate,Cache<String, String> localCache) {
        stringRedisTemplate.delete(keys);
        localCache.invalidateAll();

        keys.clear();
    }

    @Async
    @Override
    public void clearPictureFile(Picture picture) {
     pictureDomainService.clearPictureFile(picture);
    }



    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        pictureDomainService.checkPictureAuth(loginUser,picture);
    }

    @Override
    public void deletePicture(long id, User loginUser) {
        pictureDomainService.deletePicture(id,loginUser);
    }

    @Override
    public void updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser) {
        pictureDomainService.updatePicture(pictureUpdateRequest,loginUser);

    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        pictureDomainService.editPicture(pictureEditRequest,loginUser);
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String color, User loginUser) {
        return pictureDomainService.searchPictureByColor(spaceId,color,loginUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pictureEditByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        pictureDomainService.pictureEditByBatch(pictureEditByBatchRequest,loginUser);
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        return pictureDomainService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest,loginUser);
    }


}




