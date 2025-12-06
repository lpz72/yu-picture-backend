package org.lpz.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByPictureRequest implements Serializable {

    private static final long serialVersionUID = -5286803634864471210L;
    /**
     * 图片id
     */
    private Long pictureId;

}
