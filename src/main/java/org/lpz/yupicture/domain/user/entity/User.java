package org.lpz.yupicture.domain.user.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import org.lpz.yupicture.domain.user.valueobject.UserRoleEnum;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;

/**
 * 用户
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    private static final long serialVersionUID = -8590320599808356397L;
    /**
     * id (指定主键生成策略)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除 (逻辑删除)
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 校验注册参数
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     */
    public static void validUserRegister(String userAccount, String userPassword, String checkPassword) {
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount,userPassword,checkPassword), ErrorCode.PARAMS_ERROR,"参数为空");

        //账号长度不小于4
        ThrowUtils.throwIf(userAccount.length() < 4,ErrorCode.PARAMS_ERROR,"用户账号长度过短");

        //两次密码是否一致
        ThrowUtils.throwIf(!userPassword.equals(checkPassword),ErrorCode.PARAMS_ERROR,"两次密码不一致");

        //密码长度不小于8
        ThrowUtils.throwIf(userPassword.length() < 8,ErrorCode.PARAMS_ERROR,"用户密码过短");
    }

    /**
     * 校验登录参数
     * @param userAccount
     * @param userPassword
     */
    public static void validUserLogin(String userAccount, String userPassword) {
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount,userPassword),ErrorCode.PARAMS_ERROR,"参数为空");

        ThrowUtils.throwIf(userAccount.length() < 4,ErrorCode.PARAMS_ERROR,"账号错误");

        ThrowUtils.throwIf(userPassword.length() < 8,ErrorCode.PARAMS_ERROR,"密码错误");
    }

    public boolean isAdmin() {
        return UserRoleEnum.ADMIN.getValue().equals(this.getUserRole());
    }

}