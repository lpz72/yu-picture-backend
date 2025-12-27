package org.lpz.yupicturebackend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicturebackend.manager.websocket.PictureEditHandler;
import org.lpz.yupicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import org.lpz.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import org.lpz.yupicturebackend.manager.websocket.model.PictureEditResponseMessage;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 事件消费者
 */
@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserService userService;

    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();

        // 获取到消息类别
        String type = pictureEditRequestMessage.getType();

        // 根据消息类别进行不同的处理
        PictureEditMessageTypeEnum enumByValue = PictureEditMessageTypeEnum.getEnumByValue(type);
        switch (enumByValue) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                // 消息类型错误
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));

        }
    }
}
