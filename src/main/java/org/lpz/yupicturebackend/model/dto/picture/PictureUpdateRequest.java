package org.lpz.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureUpdateRequest implements Serializable {
    private static final long serialVersionUID = -8995950877718432865L;

    /**
     * id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片介绍
     */
    private String introduction;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

}
