package org.lpz.yupicturebackend.auth;


import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.lpz.yupicturebackend.auth.model.SpaceUserAuthConfig;
import org.lpz.yupicturebackend.auth.model.SpaceUserRole;
import org.lpz.yupicturebackend.model.entity.Space;
import org.lpz.yupicturebackend.model.entity.SpaceUser;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.enums.SpaceRoleEnum;
import org.lpz.yupicturebackend.model.enums.SpaceTypeEnum;
import org.lpz.yupicturebackend.service.SpaceUserService;
import org.lpz.yupicturebackend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class SpaceUserAuthManager {


    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    private static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }


    /**
     * 根据角色获取到权限列表
     * @param spaceUserRole
     * @return
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return new ArrayList<>();
        }

        // 找到匹配的角色
        SpaceUserRole userRole = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> r.getKey().equals(spaceUserRole))
                .findFirst()
                .orElse(null);

        if (userRole == null) {
            return new ArrayList<>();
        }

        return userRole.getPermissions();
    }

    /**
     * 获取权限列表
     * @param space
     * @param user
     * @return
     */
    public List<String> getPermissionList(Space space, User user) {
        
        if (user == null) {
            return new ArrayList<>();
        }
        
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());

        // 公共图库
        if (space == null) {
            if (userService.isAdmin(user)) {
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            Integer spaceType = space.getSpaceType();
            SpaceTypeEnum enumByValue = SpaceTypeEnum.getEnumByValue(spaceType);
            if (enumByValue == null) {
                return new ArrayList<>();
            }
            switch (enumByValue) {
                case PRIVATE:
                    // 私有空间，仅创建者和管理员可访问
                    if (userService.isAdmin(user) || space.getUserId().equals(user.getId())) {
                        return ADMIN_PERMISSIONS;
                    } else {
                        return new ArrayList<>();
                    }
                case TEAM:
                    // 团队空间，需通过 SpaceUser 关系获取权限
                    SpaceUser spaceUser = spaceUserService.lambdaQuery().eq(SpaceUser::getSpaceId, space.getId())
                            .eq(SpaceUser::getUserId, user.getId())
                            .select(SpaceUser::getSpaceRole)
                            .one();
                    // 用户不属于该空间
                    if (spaceUser == null) {
                        return new ArrayList<>();
                    }
                    return getPermissionsByRole(spaceUser.getSpaceRole());

            }
        }
        return new ArrayList<>();
    }
}
