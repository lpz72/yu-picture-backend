package org.lpz.yupicturebackend.api.imagesearch;

import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicturebackend.api.imagesearch.model.ImageSearchResult;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 以图搜图
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrlApi = GetImagePageUrlApi.getImagePageUrlApi(imageUrl);
        String imageFirstUrlApi = GetImageFirstUrlApi.getImageFirstUrlApi(imagePageUrlApi);
        return GetImageListApi.getImageListApi(imageFirstUrlApi);
    }

    public static void main(String[] args) {
        String imageUrl = "https://pic.code-nav.cn/user_avatar/1726470855384494081/thumbnail/4D4ywhIAZrqY6VuP.jpg";
        List<ImageSearchResult> imageSearchResults = searchImage(imageUrl);
        System.out.println(imageSearchResults.get(0));
    }

}
