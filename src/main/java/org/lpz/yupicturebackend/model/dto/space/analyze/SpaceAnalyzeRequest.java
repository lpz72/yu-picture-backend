package org.lpz.yupicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分析请求
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {
    private static final long serialVersionUID = 1186113395464009413L;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 全空间分析
     */
    private boolean queryAll;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;
}
