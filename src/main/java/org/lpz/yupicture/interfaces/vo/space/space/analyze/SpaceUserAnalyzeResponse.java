package org.lpz.yupicture.interfaces.vo.space.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户上传行为分析响应
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SpaceUserAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 6821427906377913115L;

    /**
     * 时间区间
     */
    private String period;
    /**
     * 图片数量
     */
    private Long count;

}
