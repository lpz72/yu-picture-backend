package org.lpz.yupicturebackend.manager.websocket;

import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicturebackend.auth.SpaceUserAuthManager;
import org.lpz.yupicturebackend.auth.SpaceUserPermissionConstant;
import org.lpz.yupicturebackend.model.entity.Picture;
import org.lpz.yupicturebackend.model.entity.Space;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.enums.SpaceRoleEnum;
import org.lpz.yupicturebackend.model.enums.SpaceTypeEnum;
import org.lpz.yupicturebackend.service.PictureService;
import org.lpz.yupicturebackend.service.SpaceService;
import org.lpz.yupicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * websocket握手拦截器
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 从get请求参数中获取pictureId
            String pictureId = servletRequest.getParameter("pictureId");
            if (pictureId == null) {
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在，拒绝握手");
                return false;
            }

            User user = userService.getLoginUser(servletRequest);
            if (user == null) {
                log.error("用户未登录，拒绝握手");
                return false;
            }

            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (space == null) {
                    log.error("空间不存在，拒绝握手");
                    return false;
                }
                Integer spaceType = space.getSpaceType();
                if (!spaceType.equals(SpaceTypeEnum.TEAM.getValue())) {
                    log.error("空间类型非团队空间，拒绝握手");
                    return false;
                }
            }

            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, user);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("用户无权限编辑图片，拒绝握手");
                return false;
            }

            // 将用户信息和图片信息存入attributes，供后续使用
            attributes.put("user", user);
            attributes.put("userId", user.getId());
            attributes.put("pictureId", Long.valueOf(pictureId));


        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
