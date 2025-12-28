package org.lpz.yupicture.interfaces.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicture.infrastructure.annotation.AuthCheck;
import org.lpz.yupicture.interfaces.assembler.SpaceAssembler;
import org.lpz.yupicture.shared.auth.SpaceUserAuthManager;
import org.lpz.yupicture.infrastructure.common.BaseResponse;
import org.lpz.yupicture.infrastructure.common.DeleteRequest;
import org.lpz.yupicture.infrastructure.common.ResultUtils;
import org.lpz.yupicture.domain.user.constant.UserConstant;
import org.lpz.yupicture.infrastructure.exception.BusinessException;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.lpz.yupicture.interfaces.dto.space.SpaceAddRequest;
import org.lpz.yupicture.interfaces.dto.space.SpaceEditRequest;
import org.lpz.yupicture.interfaces.dto.space.SpaceQueryRequest;
import org.lpz.yupicture.interfaces.dto.space.SpaceUpdateRequest;
import org.lpz.yupicture.domain.space.entity.Space;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.domain.space.valueobject.SpaceLevelEnum;
import org.lpz.yupicture.interfaces.vo.space.SpaceLevel;
import org.lpz.yupicture.interfaces.vo.space.SpaceVO;
import org.lpz.yupicture.application.service.SpaceApplicationService;
import org.lpz.yupicture.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
@Slf4j
public class SpaceController {


    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 创建私有空间
     *
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<Long> uploadSpace(
            SpaceAddRequest spaceAddRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null || request == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        User loginUser = userApplicationService.getLoginUser(request);
        long l = spaceApplicationService.addSpace(spaceAddRequest, loginUser);

        return ResultUtils.success(l);

    }

    /**
     * 删除空间
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || request == null, ErrorCode.PARAMS_ERROR);

        // 判断是否存在该空间
        Long id = deleteRequest.getId();
        Space space = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅管理员和创建该空间的用户可删除
        User user = userApplicationService.getLoginUser(request);
        boolean admin = user.isAdmin();
        if (!admin && !Objects.equals(space.getUserId(), user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }


        boolean b = spaceApplicationService.removeById(id);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "删除失败");


        return ResultUtils.success(b);

    }

    /**
     * 更新空间（仅管理员）
     *
     * @param spaceUpdateRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUpdateRequest == null, ErrorCode.PARAMS_ERROR);

        // 将实体类和DTO进行转换
        Space space = SpaceAssembler.toSpaceEntity(spaceUpdateRequest);

        // 校验空间
        space.validSpace(false);

        // 判断空间是否存在
        Long id = space.getId();
        Space space1 = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(space1 == null, ErrorCode.NOT_FOUND_ERROR);

        // 补充空间限额参数
        spaceApplicationService.fillSpaceBySpaceLevel(space);

        //操作数据库
        boolean b = spaceApplicationService.updateById(space);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "操作失败");


        return ResultUtils.success(b);
    }

    /**
     * 根据id获取空间（仅管理员）
     *
     * @param id
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/get")
    public BaseResponse<Space> getSpaceById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(space);
    }

    /**
     * 根据id获取空间（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        User user = userApplicationService.getLoginUser(request);
        // 验证权限，非管理员只能获取自己的空间
//        ThrowUtils.throwIf(!space.getUserId().equals(user.getId()) && !userService.isAdmin(user), ErrorCode.NO_AUTH_ERROR, "获取数据失败，没有空间权限");
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        spaceVO.setUser(userApplicationService.getUserVO(user));

        // 设置权限列表
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, user);
        // 如果没有任何权限并且非管理员，则无权限
        ThrowUtils.throwIf(!user.isAdmin() && ObjUtil.isEmpty(permissionList), ErrorCode.NO_AUTH_ERROR, "没有空间权限");
        spaceVO.setPermissionList(permissionList);

        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表（仅管理员）
     *
     * @param spaceQueryRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page")
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);

        int current = spaceQueryRequest.getCurrent();
        int size = spaceQueryRequest.getPageSize();

        QueryWrapper<Space> queryWrapper = spaceApplicationService.getQueryWrapper(spaceQueryRequest);
        Page<Space> spacePage = spaceApplicationService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(spacePage);

    }

    /**
     * 分页获取空间列表（封装类）
     *
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceQueryRequest == null || request == null, ErrorCode.PARAMS_ERROR);

        int current = spaceQueryRequest.getCurrent();
        int size = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        QueryWrapper<Space> queryWrapper = spaceApplicationService.getQueryWrapper(spaceQueryRequest);
        Page<Space> spacePage = spaceApplicationService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(spaceApplicationService.getSpaceVOPage(spacePage, request));

    }

    /**
     * 获取空间级别列表
     *
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {

        List<SpaceLevel> values = Arrays.stream(SpaceLevelEnum.values())
                .map(space -> new SpaceLevel(
                        space.getValue(),
                        space.getText(),
                        space.getMaxSize(),
                        space.getMaxCount()
                )).collect(Collectors.toList());

        return ResultUtils.success(values);

    }


    /**
     * 编辑空间（给用户用）
     *
     * @param spaceEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceEditRequest == null || request == null, ErrorCode.PARAMS_ERROR);
        // 转换
        Space space = SpaceAssembler.toSpaceEntity(spaceEditRequest);

        // 设置编辑时间
        space.setEditTime(new Date());
        // 校验
        space.validSpace(false);
        // 是否存在
        Space space1 = spaceApplicationService.getById(space.getId());
        ThrowUtils.throwIf(space1 == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人和管理员可编辑
        User user = userApplicationService.getLoginUser(request);
        if (!space1.getUserId().equals(user.getId()) && !user.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 操作数据库
        boolean b = spaceApplicationService.updateById(space);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "更新失败");


        return ResultUtils.success(true);

    }


}
