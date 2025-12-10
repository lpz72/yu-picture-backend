package org.lpz.yupicturebackend.model.dto.picture;

import lombok.Data;
import org.lpz.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;

import java.io.Serializable;
import java.util.List;

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
