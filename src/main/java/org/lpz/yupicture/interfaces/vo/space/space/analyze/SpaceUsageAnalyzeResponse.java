package org.lpz.yupicture.interfaces.vo.space.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间资源使用分析响应
 */
@Data
public class SpaceUsageAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 6821427906377913115L;

    /**
     * 已使用大小
     */
    private Long usedSize;
    /**
     * 总大小
     */
    private Long maxSize;

    /**
     * 空间使用比例
     */
    private Double sizeUsageRatio;

    /**
     * 已使用数量
     */
    private Long usedCount;

    /**
     * 总数量
     */
    private Long MaxCount;


    /**
     * 图片数量占比
     */
    private Double countUsageRatio;

}
