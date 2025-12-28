package org.lpz.yupicture.domain.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.domain.user.repository.UserRepository;
import org.lpz.yupicture.domain.user.service.UserDomainService;
import org.lpz.yupicture.domain.user.valueobject.UserRoleEnum;
import org.lpz.yupicture.infrastructure.exception.BusinessException;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.lpz.yupicture.interfaces.dto.user.UserQueryRequest;
import org.lpz.yupicture.interfaces.vo.user.UserVO;
import org.lpz.yupicture.shared.auth.StpKit;
import org.lpz.yupicture.interfaces.vo.user.LoginUserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.lpz.yupicture.domain.user.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author lenovo
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-10-10 17:03:16
*/
@Service
@Slf4j
public class UserDomainServiceImpl implements UserDomainService {

    @Resource
    private UserRepository userRepository;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {

        //1.校验,应用服务层已校验

        //2.校验账号是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        Long count = userRepository.getBaseMapper().selectCount(queryWrapper);
        ThrowUtils.throwIf(count > 0,ErrorCode.PARAMS_ERROR,"用户账号重复");

        //3.密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        //4.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = userRepository.save(user);
        ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR,"注册失败，数据库错误");

        return user.getId();
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        //盐值，混淆密码
        final String salt = "lpz";
        return DigestUtils.md5DigestAsHex((salt + userPassword).getBytes());
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {

        //1.校验,应用服务层已校验

        //2.账号是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        String encryptPassword = this.getEncryptPassword(userPassword);
        //加密
        queryWrapper.eq("userPassword",encryptPassword);
        User user = userRepository.getBaseMapper().selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在或密码错误");
        }

        //3.记录用户登录态
        request.getSession().setAttribute(USER_LOGIN_STATE,user);

        // 4. 记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE,user);

        return this.getLoginUserVO(user);
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user,loginUserVO);
        return loginUserVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        //判断用户是否登录
        Object object = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) object;
        ThrowUtils.throwIf(user == null || user.getId() == null,ErrorCode.NOT_LOGIN_ERROR);

        //从数据库中查询，获取用户的最新信息
        User lasteUser = userRepository.getBaseMapper().selectById(user.getId());
        ThrowUtils.throwIf(lasteUser == null,ErrorCode.NOT_LOGIN_ERROR);
        return lasteUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {

        //判断是否登录
        Object object = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) object;
        ThrowUtils.throwIf(user == null,ErrorCode.OPERATION_ERROR,"未登录");

        //移出登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);

        return true;
    }

    @Override
    public UserVO getUserVO(User user) {

        if (user == null) {
            return null;
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);

        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {

        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }

        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {

        ThrowUtils.throwIf(userQueryRequest == null,ErrorCode.PARAMS_ERROR,"请求参数为空");

        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public long addUser(User user) {
        // 添加用户时默认密码为12345678
        user.setUserPassword(this.getEncryptPassword("12345678"));
        boolean save = userRepository.save(user);
        ThrowUtils.throwIf(!save,ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    @Override
    public User getById(Long id) {
        return userRepository.getById(id);
    }

    @Override
    public boolean removeById(Long id) {
        return userRepository.removeById(id);
    }

    @Override
    public boolean updateById(User user) {
        return userRepository.updateById(user);
    }

    @Override
    public Page<User> page(Page<User> page, QueryWrapper<User> queryWrapper) {
        return userRepository.page(page,queryWrapper);

    }

}




