package org.lpz.yupicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间图片标签分析响应
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SpaceTagAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 6821427906377913115L;

    /**
     * 标签名称
     */
    private String tag;
    /**
     * 图片数量
     */
    private Long count;

}
