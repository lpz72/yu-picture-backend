package org.lpz.yupicturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicturebackend.annotation.AuthCheck;
import org.lpz.yupicturebackend.common.Baseresponse;
import org.lpz.yupicturebackend.common.DeleteRequest;
import org.lpz.yupicturebackend.common.ResultUtils;
import org.lpz.yupicturebackend.constant.UserConstant;
import org.lpz.yupicturebackend.exception.BusinessException;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.lpz.yupicturebackend.manager.CosManager;
import org.lpz.yupicturebackend.model.PictureTagCategory;
import org.lpz.yupicturebackend.model.dto.picture.PictureEditRequest;
import org.lpz.yupicturebackend.model.dto.picture.PictureQueryRequest;
import org.lpz.yupicturebackend.model.dto.picture.PictureUpdateRequest;
import org.lpz.yupicturebackend.model.dto.picture.PictureUploadRequest;
import org.lpz.yupicturebackend.model.entity.Picture;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.vo.PictureVO;
import org.lpz.yupicturebackend.service.PictureService;
import org.lpz.yupicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

    /**
     * 上传图片 (可重新上传)
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload")
    public Baseresponse<PictureVO> uploadFile(
            @RequestParam("file")MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(multipartFile == null || request == null,ErrorCode.PARAMS_ERROR,"请求参数为空");

        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);

        return ResultUtils.success(pictureVO);

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

        // 判断是否存在该图片
        Long id = deleteRequest.getId();
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null,ErrorCode.NOT_FOUND_ERROR);

        // 仅管理员和创建该图片的用户可删除
        User user = userService.getLoginUser(request);
        boolean admin = userService.isAdmin(user);
        if (!admin && !Objects.equals(picture.getUserId(), user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean b = pictureService.removeById(id);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR,"删除失败");

        return ResultUtils.success(b);

    }

    /**
     * 更新图片（仅管理员）
     * @param pictureUpdateRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public Baseresponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest) {
        ThrowUtils.throwIf(pictureUpdateRequest == null,ErrorCode.PARAMS_ERROR);

        // 将实体类和DTO进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest,picture);

        //将tags转换成json
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

        // 校验图片
        pictureService.validPicture(picture);

        // 判断图片是否存在
        Long id = picture.getId();
        Picture picture1 = pictureService.getById(id);
        ThrowUtils.throwIf(picture1 == null,ErrorCode.NOT_FOUND_ERROR);

        //操作数据库
        boolean b = pictureService.updateById(picture);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR,"操作失败");
        return ResultUtils.success(b);
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
    public Baseresponse<PictureVO> getPictureVOById(long id) {
        ThrowUtils.throwIf(id <= 0,ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null,ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(PictureVO.objToVo(picture));
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
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page/vo")
    public Baseresponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null || request == null,ErrorCode.PARAMS_ERROR);

        int current = pictureQueryRequest.getCurrent();
        int size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage,request));

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
        // 转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest,picture);
        // 处理tags
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 校验
        pictureService.validPicture(picture);
        // 是否存在
        Picture picture1 = pictureService.getById(picture.getId());
        ThrowUtils.throwIf(picture1 == null,ErrorCode.NOT_FOUND_ERROR);

        // 仅本人和管理员可编辑
        User user = userService.getLoginUser(request);
        if (!picture.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 操作数据库
        boolean b = pictureService.updateById(picture);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR,"更新失败");

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




}
