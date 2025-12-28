package org.lpz.yupicture.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.interfaces.dto.user.UserQueryRequest;
import org.lpz.yupicture.interfaces.vo.user.UserVO;
import org.lpz.yupicture.interfaces.vo.user.LoginUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author lenovo
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-10-10 17:03:16
*/
public interface UserDomainService {

    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return 新用户 id
     */
    long userRegister(String userAccount,String userPassword,String checkPassword);

    /**
     * 加密
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);


    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

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

    User getById(Long id);

    boolean removeById(Long id);


    boolean updateById(User user);

    Page<User> page(Page<User> page, QueryWrapper<User> queryWrapper);
}
