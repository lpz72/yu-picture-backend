package org.lpz.yupicture.infrastructure.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;
import org.lpz.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import org.lpz.yupicture.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import org.lpz.yupicture.infrastructure.exception.BusinessException;
import org.lpz.yupicture.infrastructure.exception.ErrorCode;
import org.lpz.yupicture.infrastructure.exception.ThrowUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {

    // 读配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务地址
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {

        ThrowUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR, "扩图参数为空");

        // 发送请求
        HttpRequest request = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Content-Type", "application/json")
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));

        try (HttpResponse httpResponse = request.execute()) {

            if (!httpResponse.isOk()) {
                log.error("请求异常 {} ", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String code = response.getCode();
            if (StrUtil.isNotBlank(code)) {
                String errorMessage = response.getMessage();
                log.error("AI扩图失败，code：{}，errorMessage：{}", code, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询创建的任务
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务ID为空");

        String url = String.format(GET_OUT_PAINTING_TASK_URL, taskId);

        // 发送请求
        HttpRequest request = HttpRequest.get(url)
                .header("Content-Type", "application/json")
                .header(Header.AUTHORIZATION, "Bearer " + apiKey);

        try (HttpResponse httpResponse = request.execute()) {

            if (!httpResponse.isOk()) {
                log.error("请求异常 {} ", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取AI 扩图任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);

        }
    }
}
