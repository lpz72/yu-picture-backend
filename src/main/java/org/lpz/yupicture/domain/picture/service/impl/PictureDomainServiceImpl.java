package org.lpz.yupicture.domain.picture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lpz.yupicture.domain.picture.entity.Picture;
import org.lpz.yupicture.domain.picture.reposity.PictureRepository;
import org.lpz.yupicture.domain.picture.service.PictureDomainService;
import org.lpz.yupicture.domain.picture.valueobject.PictureReviewStatusEnum;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.infrastructure.api.CosManager;
import org.lpz.yupicture.infrastructure.api.aliyunai.AliYunAiApi;
import org.lpz.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;
import org.lpz.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import org.lpz.yupicture.infrastructure.exception.BusinessException;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.lpz.yupicture.infrastructure.utils.ColorSimilarUtils;
import org.lpz.yupicture.interfaces.assembler.PictureAssembler;
import org.lpz.yupicture.interfaces.dto.picture.*;
import org.lpz.yupicture.interfaces.vo.picture.PictureVO;
import org.lpz.yupicture.infrastructure.manager.upload.FilePictureUpload;
import org.lpz.yupicture.infrastructure.manager.upload.PictureUploadTemplate;
import org.lpz.yupicture.infrastructure.manager.upload.UrlPictureUpload;
import org.lpz.yupicture.infrastructure.manager.upload.dto.file.UploadPictureResult;
import org.lpz.yupicture.domain.space.entity.Space;
import org.lpz.yupicture.application.service.SpaceApplicationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author lpz
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-10-25 17:43:32
*/
@Service
@Slf4j
public class PictureDomainServiceImpl implements PictureDomainService {

    @Resource
    private PictureRepository pictureRepository;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;


    private final Set<String> keys = new ConcurrentHashSet<>();
    @Autowired
    private CosManager cosManager;
    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private TransactionTemplate transactionTemplate;
    @Autowired
    private AliYunAiApi aliYunAiApi;


    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {

        // 只有已登录用户可以上传图片
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);


        // 判断是更新图片还是新增图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }

        Long spaceId = pictureUploadRequest.getSpaceId();
        // 如果是上传到空间
        if (spaceId != null) {
            // 判断空间是否存在且属于该用户
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");

            // 校验额度
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"空间大小不足");
            }
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"空间条数不足");
            }

        }

        // 如果是更新图片，还需判断图片是否存在
        if (pictureId != null) {
            Picture picture = pictureRepository.getBaseMapper().selectById(pictureId);
            ThrowUtils.throwIf(picture == null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
            // 删除cos的旧图片
            this.clearPictureFile(picture);
            // 仅本人或管理员可更新,已使用sa-token注解校验
//            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }

            Long oldSpaceId = picture.getSpaceId();
            // 如果更新时没有传空间id，则沿用之前的空间id
            if (spaceId == null) {
                spaceId = oldSpaceId;
            }
            // 判断当前更新的图片所上传的空间id与之前的是否一致
            if (oldSpaceId != null && !oldSpaceId.equals(spaceId)) {
                throw  new BusinessException(ErrorCode.PARAMS_ERROR,"空间id不一致");
            }

            // 如果是上传到空间更新图片的话，需要扣除之前空间的额度
            Long finalSpaceId1 = spaceId;
            if (finalSpaceId1 != null) {
                transactionTemplate.execute(status -> {
                    boolean update = spaceApplicationService.lambdaUpdate().setSql("totalSize = totalSize - " + picture.getPicSize())
                            .setSql("totalCount = totalCount - 1")
                            .eq(Space::getId, finalSpaceId1)
                            .update();
                    ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"额度更新失败");
                    return true;
                });
            }


        }


        // 按照id为每个用户指分配一个文件夹 => 按照空间id划分目录
        String prefix;
        if (spaceId == null) {
            // 没有指定空间id，则是上传到公共图库
            prefix = String.format("public/%s",loginUser.getId());
        } else {
            // 按照空间id划分目录
            prefix = String.format("space/%s",spaceId);
        }



        // 通过判断输入的类型来选择不同的上传处理器
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;

        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }

        // 构造要入库的信息
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, prefix);
        Picture picture = new Picture();
        // 补充设置spaceId
        picture.setSpaceId(spaceId);
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setName(picName);
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setUserId(loginUser.getId());
        // 设置图片主色调
        picture.setPicColor(uploadPictureResult.getPicColor());

        // 如果是更新，为其填上id和编辑时间
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        this.fillReviewParams(picture,loginUser);

        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean b = pictureRepository.saveOrUpdate(picture);
            ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR,"图片上传失败");
            if (finalSpaceId != null) {
                // 更新空间额度
                boolean update = spaceApplicationService.lambdaUpdate().setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .eq(Space::getId, finalSpaceId)
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }

            return true;
        });


        return PictureVO.objToVo(picture);
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }

        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotNull(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotNull(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotNull(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotNull(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotNull(reviewStatus),"reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotNull(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotNull(spaceId), "spaceId", spaceId);
        // 若 nullSpaceId 为 true，则查询 spaceId 为空的记录
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        // >=
        queryWrapper.ge(ObjUtil.isNotNull(startEditTime),"editTIme",startEditTime);
        // <
        queryWrapper.lt(ObjUtil.isNotNull(endEditTime),"editTime",endEditTime);

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            //拼接查询条件
            queryWrapper.and(qw -> qw.like("name",searchText)
                    .or()
                    .like("introduction",searchText)
            );
        }

        // Json数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags","\"" + tag + "\"");
            }

        }

        // 排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);


        return queryWrapper;
    }




    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);

        ThrowUtils.throwIf(id == null || pictureReviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(pictureReviewStatusEnum),
                ErrorCode.PARAMS_ERROR);

        // 判断图片是否存在
        Picture oldPicture = pictureRepository.getBaseMapper().selectById(id);
        ThrowUtils.throwIf(oldPicture == null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
        // 判断图片是否已经是该状态，避免重复审核
        ThrowUtils.throwIf(Objects.equals(oldPicture.getReviewStatus(), pictureReviewRequest.getReviewStatus()),ErrorCode.OPERATION_ERROR,"请勿重复审核");

        // 操作数据库
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest,picture);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());
        boolean b = pictureRepository.updateById(picture);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User user) {

        if (user.isAdmin()) {
            // 管理员则自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(user.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
        } else {
            // 非管理员则待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR,"抓取图片数量不能超过30张");
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 要抓取的图片地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1",searchText);

        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.info("图片抓取失败：",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"图片抓取失败");
        }
        // 获取图片
        int uploadCount = 0;
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取页面失败");
        }
        Elements elements = div.select("img.mimg");
        for (Element element : elements) {
            String fileUrl = element.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}",fileUrl);
                continue;
            }

            // 处理图片地址，防止出现转义问题
            int i = fileUrl.indexOf("?");
            if (i > -1) {
                fileUrl = fileUrl.substring(0, i);
            }

            String name = namePrefix + (uploadCount + 1);

            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setPicName(name);
            if (StrUtil.isNotBlank(namePrefix)) {
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }

            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}",pictureVO.getId());
                uploadCount ++;

            } catch (Exception e) {
                log.info("图片上传失败:",e);
            }


            if (uploadCount >= count) {
                break;
            }

        }


        return uploadCount;
    }



    @Async
    @Override
    public void clearPictureFile(Picture picture) {

        String url = picture.getUrl();
        // 判断该图片是否被多条记录引用
        Long count = pictureRepository.lambdaQuery().eq(Picture::getUrl, url).count();

        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }

        Long spaceId = picture.getSpaceId();
        // 如果是上传到空间
        if (spaceId != null) {
            int i = url.indexOf(String.format("/%s",spaceId));
            if (i > -1) {
                String key = url.substring(i);
                cosManager.deleteObject(key);
            }

            // 清理缩略图
            int j = picture.getThumbnailUrl().indexOf(String.format("/%s",spaceId));
            if (j > -1) {
                String thumbKey = picture.getThumbnailUrl().substring(j);
                cosManager.deleteObject(thumbKey);
            }
        } else {
            int i = url.indexOf("/public");
            if (i > -1) {
                String key = url.substring(i);
                cosManager.deleteObject(key);
            }

            // 清理缩略图
            int j = picture.getThumbnailUrl().indexOf("/public");
            if (j > -1) {
                String thumbKey = picture.getThumbnailUrl().substring(j);
                cosManager.deleteObject(thumbKey);
            }
        }



    }


    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        ThrowUtils.throwIf(picture == null || loginUser == null,ErrorCode.PARAMS_ERROR);
        Long spaceId = picture.getSpaceId();
        // 如果图片上传到了空间，则检查空间权限
        if (spaceId != null) {
            Space space = spaceApplicationService.getById(spaceId);
            if (!space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 公共图库，仅图片创建者和管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public void deletePicture(long id, User loginUser) {
        ThrowUtils.throwIf(id <= 0 || loginUser == null,ErrorCode.PARAMS_ERROR);

        // 获取图片
        Picture picture = pictureRepository.getBaseMapper().selectById(id);
        ThrowUtils.throwIf(picture == null,ErrorCode.NOT_FOUND_ERROR);

        // 权限校验：仅管理员和创建该图片的用户可删除、空间权限校验
//        checkPictureAuth(loginUser, picture);


        // 开启事务
        Long spaceId = picture.getSpaceId();

        transactionTemplate.execute(status -> {
            boolean b = pictureRepository.removeById(id);
            ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR,"删除失败");
            if(spaceId != null) {
                // 更新空间额度
                boolean update = spaceApplicationService.lambdaUpdate().setSql("totalSize = totalSize - " + picture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .eq(Space::getId, spaceId)
                        .update();
                ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"额度更新失败");
            }


            return true;
        });

        // 在cos中删除对应图片和缩略图
        clearPictureFile(picture);
    }

    @Override
    public void updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser) {

        // 将实体类和DTO进行转换
        Picture picture = PictureAssembler.toPictureEntity(pictureUpdateRequest);

        //将tags转换成json
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

        // 校验图片
        picture.validPicture();

        // 判断图片是否存在
        Long id = picture.getId();
        Picture picture1 = pictureRepository.getById(id);
        ThrowUtils.throwIf(picture1 == null,ErrorCode.NOT_FOUND_ERROR);

        // 补充审核状态参数
        fillReviewParams(picture,loginUser);

        //操作数据库
        boolean b = pictureRepository.updateById(picture);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR,"操作失败");

    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 转换
        Picture picture = PictureAssembler.toPictureEntity(pictureEditRequest);
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 校验
        picture.validPicture();
        // 是否存在
        Picture oldPicture = pictureRepository.getById(picture.getId());
        ThrowUtils.throwIf(oldPicture == null,ErrorCode.NOT_FOUND_ERROR);

        // 权限校验
//        checkPictureAuth(loginUser,oldPicture);

        // 补充审核状态参数
       fillReviewParams(picture,loginUser);

        // 操作数据库
        boolean b = pictureRepository.updateById(picture);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR,"更新失败");
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String color, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceId <= 0 || StrUtil.isBlank(color),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null,ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceApplicationService.getById(spaceId);
        ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()),ErrorCode.NO_AUTH_ERROR);
        // 3. 获取空间内所有图片
        List<Picture> pictureList = pictureRepository.lambdaQuery().eq(Picture::getSpaceId, spaceId).isNotNull(Picture::getPicColor).list();

        // 转换
        Color color1 = Color.decode(color);

        // 4. 计算相似度，排序，取前10个
        List<Picture> result = pictureList.stream().sorted(Comparator.comparingDouble(picture -> {
            if (StrUtil.isBlank(picture.getPicColor())) {
                return Double.MAX_VALUE;
            }
            Color color2 = Color.decode(picture.getPicColor());

            return -ColorSimilarUtils.calculateSimilarity(color1,color2);

        })).limit(10).collect(Collectors.toList());

        return result.stream().map(PictureVO::objToVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pictureEditByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 1.校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String nameRule = pictureEditByBatchRequest.getNameRule();

        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList) || spaceId == null,ErrorCode.PARAMS_ERROR);

        // 2. 空间权限校验
        Space space = spaceApplicationService.getById(spaceId);
        ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()),ErrorCode.NO_AUTH_ERROR);

        // 3. 从数据库中取出图片数据，仅取id和spaceId
        List<Picture> pictureList = pictureRepository.lambdaQuery().
                in(Picture::getId, pictureIdList)
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId,spaceId)
                .list();

        // 4. 修改数据
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            String tag = JSONUtil.toJsonStr(tags);
            if (StrUtil.isNotBlank(tag)) {
                picture.setTags(tag);
            }
        });

        // 修改图片名称
        fillPictureWithNameRule(pictureList, nameRule);

        // 5. 批量更新
        boolean b = pictureRepository.updateBatchById(pictureList);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR);
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {

        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        CreateOutPaintingTaskRequest.Parameters parameters = createPictureOutPaintingTaskRequest.getParameters();

        // 校验参数
        ThrowUtils.throwIf(pictureId <= 0 || parameters == null, ErrorCode.PARAMS_ERROR);

        // 从数据库中获取图片信息
        Picture picture = pictureRepository.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR,"图片不存在");

        // 权限校验
//        checkPictureAuth(loginUser, picture);

        // 构造请求体
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        createOutPaintingTaskRequest.setParameters(parameters);
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        createOutPaintingTaskRequest.setInput(input);

        // 调用API接口，创建任务
        return aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    public void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }

        try {
            for (int i = 0;i < pictureList.size();i ++) {
                String name = nameRule.replaceAll("\\{序号}",String.valueOf(i + 1));
                Picture picture = pictureList.get(i);
                picture.setName(name);
            }
        } catch (Exception e) {
            log.error("名称解析错误，",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"名称解析错误");
        }
    }




}




