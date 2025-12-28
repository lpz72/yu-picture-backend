package org.lpz.yupicture.infrastructure.api.imagesearch;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicture.infrastructure.exception.BusinessException;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取相似图片搜索结果页面地址
     *
     * @param imageUrl
     * @return
     */

    public static String getImagePageUrlApi(String imageUrl) {

        // 1.准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");

        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        String acsToken = "jmM4zyI8OUixvSuWh0sCy4xWbsttVMZb9qcRTmn6SuNWg0vCO7N0s6Lffec+IY5yuqHujHmCctF9BVCGYGH0H5SH/H3VPFUl4O4CP1jp8GoAzuslb8kkQQ4a21Tebge8yhviopaiK66K6hNKGPlWt78xyyJxTteFdXYLvoO6raqhz2yNv50vk4/41peIwba4lc0hzoxdHxo3OBerHP2rfHwLWdpjcI9xeu2nJlGPgKB42rYYVW50+AJ3tQEBEROlg/UNLNxY+6200B/s6Ryz+n7xUptHFHi4d8Vp8q7mJ26yms+44i8tyiFluaZAr66/+wW/KMzOhqhXCNgckoGPX1SSYwueWZtllIchRdsvCZQ8tFJymKDjCf3yI/Lw1oig9OKZCAEtiLTeKE9/CY+Crp8DHa8Tpvlk2/i825E3LuTF8EQfzjcGpVnR00Lb4/8A";


        try {
            // 2. 发送post请求到百度接口
            HttpResponse response = HttpRequest.post(url)
                    .form(formData).header("ACS-Token", acsToken)
                    .timeout(5000).execute();

            // 判断响应状态
            if (HttpStatus.HTTP_OK != response.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 解析响应
            String body = response.body();
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);

            // 3. 处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = data.get("url").toString();

            // 对url解码
            String searchUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果url为空
            if (searchUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");

            }
            return searchUrl;
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }


    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://pic.code-nav.cn/user_avatar/1726470855384494081/thumbnail/4D4ywhIAZrqY6VuP.jpg";
        String result = getImagePageUrlApi(imageUrl);
        System.out.println("搜索成功，结果URL：" + result);
    }

}
