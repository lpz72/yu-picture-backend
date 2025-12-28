package org.lpz.yupicture.shared.websocket.disruptor;

import lombok.Data;
import org.lpz.yupicture.shared.websocket.model.PictureEditRequestMessage;
import org.lpz.yupicture.domain.user.entity.User;
import org.springframework.web.socket.WebSocketSession;

@Data
public class PictureEditEvent {


    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片id
     */
    private Long pictureId;

}
