package org.lpz.yupicturebackend.model.dto.space;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lpz.yupicturebackend.common.PageRequest;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {

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
     * 创建用户 id
     */
    private Long userId;


    private static final long serialVersionUID = 1L;
}
