package org.lpz.yupicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicturebackend.auth.SpaceUserPermissionConstant;
import org.lpz.yupicturebackend.auth.annotaion.SaSpaceCheckPermission;
import org.lpz.yupicturebackend.common.BaseResponse;
import org.lpz.yupicturebackend.common.DeleteRequest;
import org.lpz.yupicturebackend.common.ResultUtils;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.lpz.yupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import org.lpz.yupicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import org.lpz.yupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import org.lpz.yupicturebackend.model.entity.SpaceUser;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.vo.SpaceUserVO;
import org.lpz.yupicturebackend.service.SpaceService;
import org.lpz.yupicturebackend.service.SpaceUserService;
import org.lpz.yupicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Autowired
    @Resource
    private SpaceUserService spaceUserService;

    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(
            SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        long l = spaceUserService.addSpaceUser(spaceUserAddRequest);

        return ResultUtils.success(l);

    }

    /**
     * 移除空间成员
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || request == null, ErrorCode.PARAMS_ERROR);

        // 判断是否存在该空间
        Long id = deleteRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        boolean b = spaceService.removeById(id);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "删除失败");

        return ResultUtils.success(b);

    }

    /**
     * 修改空间成员 （设置权限）
     *
     * @param spaceUserEditRequest
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceUserEditRequest spaceUserEditRequest) {
        ThrowUtils.throwIf(spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 将实体类和DTO进行转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);

        // 校验空间
        spaceUserService.validSpaceUser(spaceUser, false);

        // 判断空间是否存在
        Long id = spaceUser.getId();
        SpaceUser oldSpace = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);


        //操作数据库
        boolean b = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "操作失败");

        return ResultUtils.success(b);
    }

    /**
     * 查询某个用户在某个空间的信息
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<SpaceUser> getSpaceUserById(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 参数校验
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId,userId),ErrorCode.PARAMS_ERROR);


        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);

        SpaceUser one = spaceUserService.getOne(queryWrapper);
        ThrowUtils.throwIf(one == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(one);
    }


    /**
     * 查询成员信息列表
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<SpaceUserVO>> listSpaceUserVO(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null || request == null, ErrorCode.PARAMS_ERROR);

        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);

        List<SpaceUser> list = spaceUserService.list(queryWrapper);
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(list);

        return ResultUtils.success(spaceUserVOList);

    }

    /**
     * 查询我加入的团队空间列表
     *
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMySpaceTeam(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        User user = userService.getLoginUser(request);
        Long id = user.getId();

        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", id);

        List<SpaceUser> list = spaceUserService.list(queryWrapper);
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(list);

        return ResultUtils.success(spaceUserVOList);

    }

}
