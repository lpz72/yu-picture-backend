package org.lpz.yupicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import org.lpz.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import org.lpz.yupicture.interfaces.dto.picture.*;
import org.lpz.yupicture.domain.picture.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.interfaces.vo.picture.PictureVO;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author lpz
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-10-25 17:43:32
*/
public interface PictureApplicationService extends IService<Picture> {

    /**
     * 上传图片
     * @param inputSource
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, HttpServletRequest request);



    /**
     * 将查询请求转换为QueryWrapper对象
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片封装类，并关联用户信息
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片数据校验
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充图片审核相关参数
     * @param picture
     * @param user
     */
    void fillReviewParams(Picture picture,User user);

    /**
     * 批量抓取和创建图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     */
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 添加缓存键
     * @param key
     */
    void addCacheKey(String key);

    /**
     * 删除缓存键
     */
    void deleteCacheKeys(StringRedisTemplate stringRedisTemplate,Cache<String, String> localCache);

    /**
     * 删除cos中的图片文件
     * @param picture
     */
    void clearPictureFile(Picture picture);

    /**
     * 校验空间权限
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 抽象出的删除图片方法
     * @param id
     * @param loginUser
     */
    void deletePicture(long id, User loginUser);

    /**
     * 抽象出的更新图片方法
     * @param pictureUpdateRequest
     * @param loginUser
     */
    void updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser);

    /**
     * 抽象出的编辑图片方法
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 颜色搜图
     * @param spaceId
     * @param color
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId,String color,User loginUser);

    /**
     * 批量修改图片
     * @param pictureEditByBatchRequest
     * @param loginUser
     * @return
     */
    void pictureEditByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建AI扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

}
