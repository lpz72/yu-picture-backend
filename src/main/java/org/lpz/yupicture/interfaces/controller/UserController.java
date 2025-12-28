package org.lpz.yupicture.interfaces.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.lpz.yupicture.infrastructure.annotation.AuthCheck;
import org.lpz.yupicture.infrastructure.common.BaseResponse;
import org.lpz.yupicture.infrastructure.common.DeleteRequest;
import org.lpz.yupicture.infrastructure.common.ResultUtils;
import org.lpz.yupicture.interfaces.assembler.UserAssembler;
import org.lpz.yupicture.interfaces.dto.user.*;
import org.lpz.yupicture.domain.user.constant.UserConstant;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.interfaces.vo.user.LoginUserVO;
import org.lpz.yupicture.interfaces.vo.user.UserVO;
import org.lpz.yupicture.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserApplicationService userApplicationService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        return ResultUtils.success(userApplicationService.userRegister(userRegisterRequest));
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){

        LoginUserVO loginUserVO = userApplicationService.userLogin(userLoginRequest, request);
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null,ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        return ResultUtils.success(userApplicationService.getLoginUserVO(loginUser));
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null,ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userApplicationService.userLogout(request));
    }


    /**
     * 创建用户
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {

        ThrowUtils.throwIf(userAddRequest == null,ErrorCode.PARAMS_ERROR,"请求参数为空");

        User user = UserAssembler.toUserEntity(userAddRequest);
        return ResultUtils.success(userApplicationService.addUser(user));

    }

    /**
     * 根据id获取用户，仅管理员可操作
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        return ResultUtils.success(userApplicationService.getUserById(id));

    }

    /**
     * 删除用户
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {

        boolean b = userApplicationService.deleteUser(deleteRequest);
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {

        ThrowUtils.throwIf(userUpdateRequest == null,ErrorCode.PARAMS_ERROR,"请求参数为空");

        User user = UserAssembler.toUserEntity(userUpdateRequest);
        userApplicationService.updateUser(user);
        return ResultUtils.success(true);

    }

    /**
     * 根据id获取包装类（脱敏后的）
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        return ResultUtils.success(userApplicationService.getUserVOById(id));

    }

    /**
     * 分页获取用户封装列表（仅管理员）
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {

        return ResultUtils.success(userApplicationService.listUserVOByPage(userQueryRequest));

    }
}
