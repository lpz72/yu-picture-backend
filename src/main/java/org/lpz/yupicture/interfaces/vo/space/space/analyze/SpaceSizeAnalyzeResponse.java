package org.lpz.yupicture.interfaces.vo.space.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间图片大小分析响应
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SpaceSizeAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 6821427906377913115L;

    /**
     * 图片大小区间
     */
    private String sizeRange;
    /**
     * 图片数量
     */
    private Long count;

}
