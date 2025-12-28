package org.lpz.yupicture.interfaces.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicture.interfaces.assembler.SpaceUserAssembler;
import org.lpz.yupicture.shared.auth.SpaceUserPermissionConstant;
import org.lpz.yupicture.shared.auth.annotaion.SaSpaceCheckPermission;
import org.lpz.yupicture.infrastructure.common.BaseResponse;
import org.lpz.yupicture.infrastructure.common.DeleteRequest;
import org.lpz.yupicture.infrastructure.common.ResultUtils;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.lpz.yupicture.interfaces.dto.space.spaceuser.SpaceUserAddRequest;
import org.lpz.yupicture.interfaces.dto.space.spaceuser.SpaceUserEditRequest;
import org.lpz.yupicture.interfaces.dto.space.spaceuser.SpaceUserQueryRequest;
import org.lpz.yupicture.domain.space.entity.SpaceUser;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.interfaces.vo.space.SpaceUserVO;
import org.lpz.yupicture.application.service.SpaceApplicationService;
import org.lpz.yupicture.application.service.SpaceUserApplicationService;
import org.lpz.yupicture.application.service.UserApplicationService;
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
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Autowired
    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

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

        long l = spaceUserApplicationService.addSpaceUser(spaceUserAddRequest);

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
        SpaceUser oldSpaceUser = spaceUserApplicationService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        boolean b = spaceUserApplicationService.removeById(id);
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
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest) {
        ThrowUtils.throwIf(spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 将实体类和DTO进行转换
        SpaceUser spaceUser = SpaceUserAssembler.toSpaceUserEntity(spaceUserEditRequest);

        // 校验空间
        spaceUserApplicationService.validSpaceUser(spaceUser, false);

        // 判断空间是否存在
        Long id = spaceUser.getId();
        SpaceUser oldSpace = spaceUserApplicationService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);


        //操作数据库
        boolean b = spaceUserApplicationService.updateById(spaceUser);
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


        QueryWrapper<SpaceUser> queryWrapper = spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest);

        SpaceUser one = spaceUserApplicationService.getOne(queryWrapper);
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

        QueryWrapper<SpaceUser> queryWrapper = spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest);

        List<SpaceUser> list = spaceUserApplicationService.list(queryWrapper);
        List<SpaceUserVO> spaceUserVOList = spaceUserApplicationService.getSpaceUserVOList(list);

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

        User user = userApplicationService.getLoginUser(request);
        Long id = user.getId();

        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", id);

        List<SpaceUser> list = spaceUserApplicationService.list(queryWrapper);
        List<SpaceUserVO> spaceUserVOList = spaceUserApplicationService.getSpaceUserVOList(list);

        return ResultUtils.success(spaceUserVOList);

    }

}
