package org.lpz.yupicture.interfaces.dto.picture;

import lombok.Data;
import org.lpz.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;

import java.io.Serializable;

/**
 * AI 扩图请求类
 */
@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    /**
     * id
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;
}
