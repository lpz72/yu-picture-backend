package org.lpz.yupicture.infrastructure.api.imagesearch.model;

import lombok.Data;

@Data
public class ImageSearchResult {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 图片来源地址
     */
    private String fromUrl;

}
