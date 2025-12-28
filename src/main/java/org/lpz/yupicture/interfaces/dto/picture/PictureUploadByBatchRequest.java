package org.lpz.yupicture.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量抓取请求体
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {
    private static final long serialVersionUID = 2333776767224242829L;

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix;

}
