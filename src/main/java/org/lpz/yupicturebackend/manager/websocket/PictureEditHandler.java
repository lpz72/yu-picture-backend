package org.lpz.yupicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.lpz.yupicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import org.lpz.yupicturebackend.manager.websocket.model.PictureEditActionTypeEnum;
import org.lpz.yupicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import org.lpz.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import org.lpz.yupicturebackend.manager.websocket.model.PictureEditResponseMessage;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.vo.UserVO;
import org.lpz.yupicturebackend.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片编辑 WebSocket 处理器
 */
@Component
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    // 每张图片的编辑状态，key：pictureId，value：正在编辑的用户ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key：pictureId，value：用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 连接建立后
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        // 保存会话到集合中
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        // 构造响应信息
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setMessage(String.format("用户 %s 加入编辑", user.getUserName()));
        pictureEditResponseMessage.setUser(userService.getUserVO(user));

        // 广播给同一张照片下的其他用户
        broadcastToPicture(pictureId, pictureEditResponseMessage);


    }

    /**
     * 处理客户端向服务器发送的消息
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");

        // 将消息解析为PictureEditRequestMessage
        String str = message.getPayload();
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(str, PictureEditRequestMessage.class);

        // 生产消息
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage,session,user,pictureId);

        // 根据消息类别进行不同的处理
//        PictureEditMessageTypeEnum enumByValue = PictureEditMessageTypeEnum.getEnumByValue(type);
//        switch (enumByValue) {
//            case ENTER_EDIT:
//                handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
//                break;
//            case EDIT_ACTION:
//                handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
//                break;
//            case EXIT_EDIT:
//                handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
//                break;
//            default:
//                // 消息类型错误
//                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
//                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
//                pictureEditResponseMessage.setMessage("消息类型错误");
//                pictureEditResponseMessage.setUser(userService.getUserVO(user));
//                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
//
//        }

    }


    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {

        // 没有用户正在编辑时，才能编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 设置当前用户为编辑用户
            pictureEditingUsers.put(pictureId, user.getId());
            // 构造发送给其他客户端的信息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            pictureEditResponseMessage.setEditAction(pictureEditRequestMessage.getEditAction());
            pictureEditResponseMessage.setMessage(String.format("用户 %s 开始编辑图片", user.getUserName()));
            pictureEditResponseMessage.setUser(userService.getUserVO(user));

            // 向其他客户端广播消息
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        } else {
            // 由用户编辑，若不是自己，仍返回提示信息
            Long editingUserId = pictureEditingUsers.get(pictureId);
            if (!editingUserId.equals(user.getId())) {

                // 获取到正在编辑的用户
                User editingUser = userService.getById(editingUserId);

                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
                pictureEditResponseMessage.setMessage(String.format("用户 %s 正在编辑图片，请稍后重试", editingUser.getUserName()));
                pictureEditResponseMessage.setUser(userService.getUserVO(editingUser));

                // 仅向当前用户广播
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
                }

            }
        }

    }

    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {

        Long editorId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionTypeEnum enumByValue = PictureEditActionTypeEnum.getEnumByValue(editAction);
        if (enumByValue == null) {
            return;
        }

        // 判断当前编辑者是否是当前用户
        if (editorId != null && editorId.equals(user.getId())) {

            // 构造发送给其他客户端的信息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            pictureEditResponseMessage.setEditAction(pictureEditRequestMessage.getEditAction());
            pictureEditResponseMessage.setMessage(String.format("用户 %s 执行 %s", user.getUserName(), enumByValue.getText()));
            pictureEditResponseMessage.setUser(userService.getUserVO(user));

            // 向其他用户广播，不包括自己，否则会造成重复编辑
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }


    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {

        Long editorId = pictureEditingUsers.get(pictureId);
        if (editorId != null && editorId.equals(user.getId())) {
            // 移除编辑状态
            pictureEditingUsers.remove(pictureId);

            // 构造消息
            // 构造发送给其他客户端的响应信息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            pictureEditResponseMessage.setEditAction(pictureEditRequestMessage.getEditAction());
            pictureEditResponseMessage.setMessage(String.format("用户 %s 退出编辑状态", user.getUserName()));
            pictureEditResponseMessage.setUser(userService.getUserVO(user));

            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }


    }

    /**
     * 连接关闭后要做的事
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");

        // 移除编辑状态
        handleExitEditMessage(null, session, user, pictureId);

        // 从session集合中删除该会话
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(webSocketSessions)) {
            webSocketSessions.remove(session);
            if (webSocketSessions.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }

        // 构造响应消息
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setMessage(String.format("用户 %s 离开编辑", user.getUserName()));
        pictureEditResponseMessage.setUser(userService.getUserVO(user));

        broadcastToPicture(pictureId, pictureEditResponseMessage);

    }


    /**
     * 排除自己的广播
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession
     * @throws IOException
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessions)) {
            // 创建ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化，将Long 类型转化为 String 类型，解决精度丢失问题
            SimpleModule simpleModule = new SimpleModule();
            simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
            // 支持long基本类型
            simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

            objectMapper.registerModule(simpleModule);
            // 序列化为JSON字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession sess : sessions) {

                // 排除掉的session不发送
                if (excludeSession != null && excludeSession.equals(sess)) {
                    continue;
                }

                if (sess.isOpen()) {
                    // 向客户端发送消息
                    sess.sendMessage(textMessage);
                }
            }
        }

    }

    /**
     * 不排除自己的广播，全部广播
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @throws IOException
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);

    }
}