package org.lpz.yupicturebackend.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureTagCategory implements Serializable {

    private static final long serialVersionUID = -5441087337465021795L;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 分类
     */
    private List<String> category;

}
