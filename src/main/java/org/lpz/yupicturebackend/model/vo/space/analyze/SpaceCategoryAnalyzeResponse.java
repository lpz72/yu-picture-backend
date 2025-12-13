package org.lpz.yupicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间图片分类分析响应
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SpaceCategoryAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 6821427906377913115L;

    /**
     * 分类
     */
    private String category;
    /**
     * 图片数量
     */
    private Long count;


    /**
     * 总大小
     */
    private Long totalSize;

}
