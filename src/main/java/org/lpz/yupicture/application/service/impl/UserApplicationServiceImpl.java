package org.lpz.yupicture.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicture.domain.user.repository.UserRepository;
import org.lpz.yupicture.domain.user.service.UserDomainService;
import org.lpz.yupicture.infrastructure.common.DeleteRequest;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.lpz.yupicture.interfaces.dto.user.UserLoginRequest;
import org.lpz.yupicture.interfaces.dto.user.UserQueryRequest;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.interfaces.dto.user.UserRegisterRequest;
import org.lpz.yupicture.interfaces.vo.user.LoginUserVO;
import org.lpz.yupicture.interfaces.vo.user.UserVO;
import org.lpz.yupicture.application.service.UserApplicationService;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Set;

/**
* @author lenovo
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-10-10 17:03:16
*/
@Service
@Slf4j
public class UserApplicationServiceImpl implements UserApplicationService {


    @Resource
    private UserDomainService userDomainService;

    @Resource
    private UserRepository userRepository;

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();


        //校验
        User.validUserRegister(userAccount,userPassword,checkPassword);

        return userDomainService.userRegister(userAccount,userPassword,checkPassword);
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {

        ThrowUtils.throwIf(request == null,ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        // 校验
        User.validUserLogin(userAccount,userPassword);

        return userDomainService.userLogin(userAccount,userPassword,request);
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        return userDomainService.getEncryptPassword(userPassword);
    }



    @Override
    public LoginUserVO getLoginUserVO(User user) {
        return userDomainService.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        return userDomainService.userLogout(request);
    }

    @Override
    public UserVO getUserVO(User user) {
        return userDomainService.getUserVO(user);
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return userDomainService.getUserVOList(userList);
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return userDomainService.getQueryWrapper(userQueryRequest);
    }

    @Override
    public long addUser(User user) {
        return userDomainService.addUser(user);
    }

    @Override
    public User getUserById(Long id) {
        ThrowUtils.throwIf(id <= 0,ErrorCode.PARAMS_ERROR);

        User user = userDomainService.getById(id);
        ThrowUtils.throwIf(user == null,ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public UserVO getUserVOById(Long id) {
        ThrowUtils.throwIf(id <= 0,ErrorCode.PARAMS_ERROR);

        User user = userDomainService.getById(id);
        ThrowUtils.throwIf(user == null,ErrorCode.NOT_FOUND_ERROR);
        return getUserVO(user);
    }

    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0,ErrorCode.PARAMS_ERROR);

        boolean b = userDomainService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public void updateUser(User user) {
        boolean b = userDomainService.updateById(user);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null,ErrorCode.PARAMS_ERROR);

        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();

        Page<User> userPage = userDomainService.page(new Page<>(current, pageSize), userDomainService.getQueryWrapper(userQueryRequest));

        Page<UserVO> userVOPage = new Page<>(current,pageSize,userPage.getTotal());

        List<UserVO> userVOList = userDomainService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }


    @Override
    public List<User> listByIds(Set<Long> userIdList) {
        return userRepository.listByIds(userIdList);
    }





}




