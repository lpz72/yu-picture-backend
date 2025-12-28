package org.lpz.yupicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.lpz.yupicture.infrastructure.common.DeleteRequest;
import org.lpz.yupicture.interfaces.dto.user.UserLoginRequest;
import org.lpz.yupicture.interfaces.dto.user.UserQueryRequest;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.interfaces.dto.user.UserRegisterRequest;
import org.lpz.yupicture.interfaces.vo.user.LoginUserVO;
import org.lpz.yupicture.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
* @author lenovo
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-10-10 17:03:16
*/
public interface UserApplicationService {

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 加密
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);


    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 登录用户信息脱敏
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 用户信息脱敏
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 用户信息脱敏 - 列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 将查询请求转换为QueryWrapper对象
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);


    long addUser(User user);

    User getUserById(Long id);

    UserVO getUserVOById(Long id);

    boolean deleteUser (DeleteRequest deleteRequest);

    void updateUser(User user);

    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    List<User> listByIds (Set<Long> userIdList);
}
