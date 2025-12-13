package org.lpz.yupicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 空间使用排行分析请求
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = 8058606974239887233L;
    /**
     * 排行前N，默认10
     */
    private Integer topN = 10;

}
