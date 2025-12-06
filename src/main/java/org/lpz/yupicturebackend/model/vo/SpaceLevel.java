package org.lpz.yupicturebackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.lpz.yupicturebackend.model.entity.Space;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
public class SpaceLevel implements Serializable {
    private static final long serialVersionUID = 1360315907521448152L;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private int value;

    /**
     * 空间级别描述
     */
    private String text;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;


}
