package org.lpz.yupicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicturebackend.annotation.AuthCheck;
import org.lpz.yupicturebackend.api.aliyunai.AliYunAiApi;
import org.lpz.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import org.lpz.yupicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import org.lpz.yupicturebackend.api.imagesearch.ImageSearchApiFacade;
import org.lpz.yupicturebackend.api.imagesearch.model.ImageSearchResult;
import org.lpz.yupicturebackend.common.Baseresponse;
import org.lpz.yupicturebackend.common.DeleteRequest;
import org.lpz.yupicturebackend.common.ResultUtils;
import org.lpz.yupicturebackend.constant.UserConstant;
import org.lpz.yupicturebackend.exception.BusinessException;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.lpz.yupicturebackend.manager.CosManager;
import org.lpz.yupicturebackend.model.entity.Space;
import org.lpz.yupicturebackend.model.enums.PictureReviewStatusEnum;
import org.lpz.yupicturebackend.model.PictureTagCategory;
import org.lpz.yupicturebackend.model.dto.picture.*;
import org.lpz.yupicturebackend.model.entity.Picture;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.vo.PictureVO;
import org.lpz.yupicturebackend.service.PictureService;
import org.lpz.yupicturebackend.service.SpaceService;
import org.lpz.yupicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private CosManager cosManager;

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();
    @Autowired
    private SpaceService spaceService;
    @Autowired
    private AliYunAiApi aliYunAiApi;


    /**
     * 本地上传图片 (可重新上传)
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload")
    public Baseresponse<PictureVO> uploadPicture(
            @RequestParam("file")MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(multipartFile == null || request == null,ErrorCode.PARAMS_ERROR,"请求参数为空");

        // 清理缓存
        pictureService.deleteCacheKeys(stringRedisTemplate, LOCAL_CACHE);

        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);

        return ResultUtils.success(pictureVO);

    }

    /**
     * 通过url上传图片 (可重新上传)
     * @param pictureUploadRequest
     * @param request
     * @return
     */
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload/url")
    public Baseresponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadRequest == null || request == null,ErrorCode.PARAMS_ERROR,"请求参数为空");

        // 清理缓存
        pictureService.deleteCacheKeys(stringRedisTemplate, LOCAL_CACHE);

        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);

        return ResultUtils.success(pictureVO);

    }

    /**
     * 审核图片（仅管理员）
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/review")
    public Baseresponse<Boolean> doPictureReview(
            PictureReviewRequest pictureReviewRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null || request == null,ErrorCode.PARAMS_ERROR,"请求参数为空");

        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest,loginUser);

        return ResultUtils.success(true);

    }

    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public Baseresponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || request == null,ErrorCode.PARAMS_ERROR);

        User user = userService.getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), user);
        // 清除缓存
        pictureService.deleteCacheKeys(stringRedisTemplate, LOCAL_CACHE);
        return ResultUtils.success(true);

    }

    /**
     * 更新图片（仅管理员）
     * @param pictureUpdateRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public Baseresponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUpdateRequest == null,ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        pictureService.updatePicture(pictureUpdateRequest, loginUser);
        // 清除缓存
        pictureService.deleteCacheKeys(stringRedisTemplate, LOCAL_CACHE);

        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片（仅管理员）
     * @param id
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/get")
    public Baseresponse<Picture> getPictureById(long id) {
        ThrowUtils.throwIf(id <= 0,ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null,ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }

    /**
     * 根据id获取图片（封装类）
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public Baseresponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0,ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null,ErrorCode.NOT_FOUND_ERROR);

        // 权限校验，仅空间管理员可以查看空间内的图片
        pictureService.checkPictureAuth(userService.getLoginUser(request), picture);

        PictureVO pictureVO = PictureVO.objToVo(picture);
        User user = userService.getById(pictureVO.getUserId());
        pictureVO.setUser(userService.getUserVO(user));
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表（仅管理员）
     * @param pictureQueryRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page")
    public Baseresponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null,ErrorCode.PARAMS_ERROR);

        int current = pictureQueryRequest.getCurrent();
        int size = pictureQueryRequest.getPageSize();

        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(picturePage);

    }

    /**
     * 分页获取图片列表（封装类）
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public Baseresponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null || request == null,ErrorCode.PARAMS_ERROR);

        int current = pictureQueryRequest.getCurrent();
        int size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 普通用户只能看到已通过审核的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage,request));

    }

    /**
     * 分页获取图片列表（封装类，缓存）
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo/cache")
    public Baseresponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null || request == null,ErrorCode.PARAMS_ERROR);

        int current = pictureQueryRequest.getCurrent();
        int size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);


        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();

        // 将查询条件转换成json字符串
        String query = JSONUtil.toJsonStr(pictureQueryRequest);
        // 构造缓存key，进行MD5加密，避免key过长
        String key = "yupicture:listPictureVOByPage:" + DigestUtils.md5DigestAsHex(query.getBytes());
        // 1. 先访问本地缓存
        String cache = LOCAL_CACHE.getIfPresent(key);
        if (cache != null) {
            // 本地有缓存，则直接返回
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cache, Page.class);
            return ResultUtils.success(pictureVOPage);
        }

        // 2. 本地没缓存，则访问redis缓存
        cache = opsForValue.get(key);
        if (cache != null) {
            // redis有缓存，则直接返回，并存入本地缓存
            LOCAL_CACHE.put(key,cache);
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cache, Page.class);
            return ResultUtils.success(pictureVOPage);
        }

        // 3. redis也没有缓存，则访问数据库
        // 判断是否传递了spaceId
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId != null) {
            // 有传递spaceId，进行权限校验
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()), ErrorCode.NO_AUTH_ERROR, "没有空间权限");
        } else {
            // 访问公共图库
            // 普通用户只能看到已通过审核的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);

        }
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

        // 4. 更新缓存
        // redis 设置缓存，过期时间为5 - 10分钟，避免缓存雪崩
        // 加入keys集合，方便后续清理缓存
        pictureService.addCacheKey(key);
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        opsForValue.set(key,cacheValue,300 + RandomUtil.randomInt(0,300), TimeUnit.SECONDS);
        // 同时设置caffeine本地缓存
        LOCAL_CACHE.put(key,cacheValue);
        return ResultUtils.success(pictureVOPage);

    }

    /**
     * 编辑图片（给用户用）
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public Baseresponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditRequest == null || request == null,ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, user);
        // 清除缓存
        pictureService.deleteCacheKeys(stringRedisTemplate, LOCAL_CACHE);
        return ResultUtils.success(true);

    }

    /**
     * 用于获取预制标签和分类
     * @return
     */
    @GetMapping("/tags_category")
    public Baseresponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tags = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> category = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTags(tags);
        pictureTagCategory.setCategory(category);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 批量上传图片（仅管理员）
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload/batch")
    public Baseresponse<Integer> uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null || request == null,ErrorCode.PARAMS_ERROR,"请求参数为空");

        User loginUser = userService.getLoginUser(request);
        int count = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        pictureService.deleteCacheKeys(stringRedisTemplate, LOCAL_CACHE);

        return ResultUtils.success(count);

    }

    /**
     * 以图搜图
     *
     * @param searchPictureByPictureRequest
     * @return
     */
    @PostMapping("/search/picture")
    public Baseresponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {

        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);

        Long pictureId = searchPictureByPictureRequest.getPictureId();
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        String url = picture.getUrl();
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImage(url);

        return ResultUtils.success(imageSearchResults);

    }

    /**
     * 颜色搜图
     *
     * @param searchPictureByColorRequest
     * @param request
     * @return
     */
    @PostMapping("/search/color")
    public Baseresponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {

        ThrowUtils.throwIf(searchPictureByColorRequest == null || request == null, ErrorCode.PARAMS_ERROR);

        Long spaceId = searchPictureByColorRequest.getSpaceId();
        String picColor = searchPictureByColorRequest.getPicColor();

        User loginUser = userService.getLoginUser(request);

        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);

        return ResultUtils.success(pictureVOList);

    }

    /**
     * 批量修改图片
     *
     * @param pictureEditByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    public Baseresponse<Boolean> PictureEditByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {

        ThrowUtils.throwIf(pictureEditByBatchRequest == null || request == null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);

        pictureService.pictureEditByBatch(pictureEditByBatchRequest,loginUser);
        // 清理缓存
        pictureService.deleteCacheKeys(stringRedisTemplate, LOCAL_CACHE);

        return ResultUtils.success(true);

    }

    /**
     * 创建AI扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param request
     * @return
     */
    @PostMapping("/out_painting/create_task")
    public Baseresponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, HttpServletRequest request) {

        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null || request == null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);

        CreateOutPaintingTaskResponse pictureOutPaintingTask = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);

        return ResultUtils.success(pictureOutPaintingTask);

    }

    /**
     * 查询AI扩图任务
     * @param taskId
     * @return
     */
    @GetMapping("/out_painting/get_task")
    public Baseresponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {

        ThrowUtils.throwIf(taskId == null, ErrorCode.PARAMS_ERROR);

        return ResultUtils.success(aliYunAiApi.getOutPaintingTask(taskId));

    }


}
