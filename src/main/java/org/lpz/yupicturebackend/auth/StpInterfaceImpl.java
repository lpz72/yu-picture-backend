package org.lpz.yupicturebackend.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import org.checkerframework.checker.units.qual.A;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.lpz.yupicturebackend.model.entity.Picture;
import org.lpz.yupicturebackend.model.entity.Space;
import org.lpz.yupicturebackend.model.entity.SpaceUser;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.enums.SpaceRoleEnum;
import org.lpz.yupicturebackend.model.enums.SpaceTypeEnum;
import org.lpz.yupicturebackend.service.PictureService;
import org.lpz.yupicturebackend.service.SpaceService;
import org.lpz.yupicturebackend.service.SpaceUserService;
import org.lpz.yupicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.lpz.yupicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    /**
     * 返回一个账号所拥有的权限码集合 
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 判断是否是space类型，仅对space类型的账号进行权限处理
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }

        // 管理员权限
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());

        // 获取上下文
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 如果所有字段都为空，则表示查询公共图库，默认通过
        if (isAllFieldsNull(contextPath)) {
            return ADMIN_PERMISSIONS;
        }

        // 获取user
        User user = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR,"用户未登录");

        Long userId = user.getId();
        // 优先获取SpaceUser对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

        // 获取spaceUserId，通过spaceUserId获取 SpaceUser 对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            ThrowUtils.throwIf(spaceUser == null,ErrorCode.NOT_FOUND_ERROR,"未找到空间用户信息");

            // 获取当前登录用户的 SpaceUser 对象
            SpaceUser one = spaceUserService.lambdaQuery().eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();

            // 当前用户不属于该空间
            if (one == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(one.getSpaceRole());
        }

        // 如果没有spaceUserId，则尝试通过spaceId和pictureId获取到space对象
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 没有spaceId，则通过pictureId获取space对象
            Long pictureId = authContext.getPictureId();
            // 图片id也没有，默认通过权限
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId,pictureId)
                    .select(Picture::getId,Picture::getSpaceId,Picture::getUserId)
                    .one();

            ThrowUtils.throwIf(picture == null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
            spaceId = picture.getSpaceId();
            // 公共图库
            if (spaceId == null) {
                // 仅本人和管理员可操作，否则仅可查看
                if (!user.getId().equals(picture.getUserId()) && !userService.isAdmin(user)) {
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                } else {
                    return ADMIN_PERMISSIONS;
                }


            }

        }

        // spaceId 不为 null，获取space对象
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        Integer spaceType = space.getSpaceType();
        if (spaceType.equals(SpaceTypeEnum.PRIVATE.getValue())) {
            // 如果是私人空间，仅创建者或管理员有权限
            if (!user.getId().equals(space.getUserId()) && !userService.isAdmin(user)) {
                return new ArrayList<>();
            } else {
                return ADMIN_PERMISSIONS;
            }
        } else {
            // 如果是团队空间，则通过spaceId和userId获取SpaceUser对象
            SpaceUser teamSpaceUser = spaceUserService.lambdaQuery().eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .select(SpaceUser::getId, SpaceUser::getSpaceRole)
                    .one();
            // 用户不属于该空间
            if (teamSpaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(teamSpaceUser.getSpaceRole());
        }

    }

    public boolean isAllFieldsNull(Object object) {
       if (object == null) {
           return true; // 对象本身为 null
       }
       // 获取对象的所有字段并判断是否都为空
       return Arrays.stream(ReflectUtil.getFields(object.getClass()))
               // 获取每个字段的值
               .map(field -> ReflectUtil.getFieldValue(object, field))
               // 检查是否所有字段都为空
               .allMatch(ObjectUtil::isEmpty);
    }


    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 本 list 仅做模拟，实际项目中要根据具体业务逻辑来查询角色
        List<String> list = new ArrayList<String>();    
        list.add("admin");
        list.add("super-admin");
        return list;
    }


    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 从请求中获取上下文
     * @return
     */
    private SpaceUserAuthContext getAuthContextByRequest() {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext spaceUserAuthContext;
        // 兼容get和post操作
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            spaceUserAuthContext = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            spaceUserAuthContext = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }

        // 根据请求路径区分id字段的含义
        Long id = spaceUserAuthContext.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestURI = request.getRequestURI();
            // /api/space/xxx
            String replace = requestURI.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(replace, "/", false);

            switch (moduleName) {
                case "picture":
                    spaceUserAuthContext.setPictureId(id);
                    break;
                case "spaceUser":
                    spaceUserAuthContext.setSpaceUserId(id);
                    break;
                case "space":
                    spaceUserAuthContext.setSpaceId(id);
                    break;
                default:
            }
        }

        return spaceUserAuthContext;
    }

}