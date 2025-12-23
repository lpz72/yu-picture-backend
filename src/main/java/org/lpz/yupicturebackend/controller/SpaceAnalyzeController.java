package org.lpz.yupicturebackend.controller;

import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicturebackend.common.BaseResponse;
import org.lpz.yupicturebackend.common.ResultUtils;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.lpz.yupicturebackend.model.dto.space.analyze.*;
import org.lpz.yupicturebackend.model.entity.Space;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.vo.space.analyze.*;
import org.lpz.yupicturebackend.service.PictureService;
import org.lpz.yupicturebackend.service.SpaceAnalyzeService;
import org.lpz.yupicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
@Slf4j
public class SpaceAnalyzeController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;


    /**
     * 空间资源使用分析
     * @param spaceUsageAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(
            @RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
            HttpServletRequest request
            ) {

        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null || request == null,ErrorCode.PARAMS_ERROR);

        User user = userService.getLoginUser(request);

        SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = spaceAnalyzeService.spaceUsageAnalyze(spaceUsageAnalyzeRequest, user);
        return ResultUtils.success(spaceUsageAnalyzeResponse);

    }

    /**
     * 空间图片分类使用分析
     * @param spaceCategoryAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(
            @RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
            HttpServletRequest request
    ) {

        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null || request == null,ErrorCode.PARAMS_ERROR);

        User user = userService.getLoginUser(request);

        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeResponses = spaceAnalyzeService.spaceCategoryAnalyze(spaceCategoryAnalyzeRequest, user);
        return ResultUtils.success(spaceCategoryAnalyzeResponses);

    }

    /**
     * 空间图片分类使用分析
     * @param spaceTagAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(
            @RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
            HttpServletRequest request
    ) {

        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null || request == null,ErrorCode.PARAMS_ERROR);

        User user = userService.getLoginUser(request);

        List<SpaceTagAnalyzeResponse> spaceTagAnalyzeResponses = spaceAnalyzeService.spaceTagAnalyze(spaceTagAnalyzeRequest, user);
        return ResultUtils.success(spaceTagAnalyzeResponses);

    }

    /**
     * 空间图片大小分析
     * @param spaceSizeAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(
            @RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
            HttpServletRequest request
    ) {

        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null || request == null,ErrorCode.PARAMS_ERROR);

        User user = userService.getLoginUser(request);

        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyzeResponses = spaceAnalyzeService.spaceSizeAnalyze(spaceSizeAnalyzeRequest, user);
        return ResultUtils.success(spaceSizeAnalyzeResponses);
    }


    /**
     * 用户上传行为分析
     * @param spaceUserAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(
            @RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
            HttpServletRequest request
    ) {

        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null || request == null,ErrorCode.PARAMS_ERROR);

        User user = userService.getLoginUser(request);

        List<SpaceUserAnalyzeResponse> spaceUserAnalyzeResponses = spaceAnalyzeService.spaceUserAnalyze(spaceUserAnalyzeRequest, user);
        return ResultUtils.success(spaceUserAnalyzeResponses);
    }

    /**
     * 空间使用排行分析
     * @param spaceRankAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/rank")
    public BaseResponse<List<Space>> getSpaceRankAnalyze(
            @RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
            HttpServletRequest request
    ) {

        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null || request == null,ErrorCode.PARAMS_ERROR);

        User user = userService.getLoginUser(request);

        List<Space> spaces = spaceAnalyzeService.spaceRankingAnalyze(spaceRankAnalyzeRequest, user);
        return ResultUtils.success(spaces);
    }

}
