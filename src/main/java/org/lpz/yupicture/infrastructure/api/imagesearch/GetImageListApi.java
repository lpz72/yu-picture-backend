package org.lpz.yupicture.infrastructure.api.imagesearch;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicture.infrastructure.api.imagesearch.model.ImageSearchResult;
import org.lpz.yupicture.infrastructure.exception.BusinessException;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;

import java.util.List;
import java.util.Map;

@Slf4j
public class GetImageListApi {

    /**
     * 获取图片列表
     *
     * @param url
     * @return
     */

    public static List<ImageSearchResult> getImageListApi(String url) {

        try {
            // 1. 发送post请求到百度接口
            HttpResponse response = HttpRequest.get(url)
                    .timeout(5000).execute();

            // 判断响应状态
            if (HttpStatus.HTTP_OK != response.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 解析响应
            String body = response.body();
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);
            JSONObject jsonObject = new JSONObject(body);

            // 2. 处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

            // 3.获取数据，并解析为json
            if (!jsonObject.containsKey("data")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
            }
            JSONObject data = new JSONObject(jsonObject.get("data"));

            if (!data.containsKey("list")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
            }
            JSONArray list = data.getJSONArray("list");
            return JSONUtil.toList(list, ImageSearchResult.class);
        } catch (Exception e) {
            log.error("获取图片列表失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }


    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=3776470526973022322&sign=121e55d2a72c00e9a780a01764670040&tk=8e88d&tpl_from=pc";
        List<ImageSearchResult> result = getImageListApi(imageUrl);
        System.out.println("搜索成功，结果：" + result.get(0));
    }

}
