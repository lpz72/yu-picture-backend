package org.lpz.yupicture.interfaces.assembler;

import cn.hutool.json.JSONUtil;
import org.lpz.yupicture.domain.picture.entity.Picture;
import org.lpz.yupicture.interfaces.dto.picture.PictureEditRequest;
import org.lpz.yupicture.interfaces.dto.picture.PictureUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 图片对象转换类
 */
public class PictureAssembler {

    public static Picture toPictureEntity(PictureEditRequest pictureEditRequest) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest,picture);
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        return picture;
    }

    public static Picture toPictureEntity(PictureUpdateRequest pictureUpdateRequest) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest,picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        return picture;
    }
}
