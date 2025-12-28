package org.lpz.yupicture.interfaces.dto.space;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SpaceUpdateRequest implements Serializable {
    private static final long serialVersionUID = -8995950877718432865L;

    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间最大大小
     */
    private Long maxSize;

    /**
     * 空间图片最大数量
     */
    private Long maxCount;


}
