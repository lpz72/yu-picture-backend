package org.lpz.yupicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.lpz.yupicturebackend.exception.BusinessException;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.lpz.yupicturebackend.mapper.PictureMapper;
import org.lpz.yupicturebackend.model.dto.space.analyze.*;
import org.lpz.yupicturebackend.model.entity.Picture;
import org.lpz.yupicturebackend.model.entity.Space;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.vo.space.analyze.*;
import org.lpz.yupicturebackend.service.PictureService;
import org.lpz.yupicturebackend.service.SpaceAnalyzeService;
import org.lpz.yupicturebackend.service.SpaceService;
import org.lpz.yupicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lpz
 * @createDate 2025-10-25 17:43:32
 */
@Service
@Slf4j
public class SpaceAnalyzeServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements SpaceAnalyzeService {


    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;
    @Autowired
    private PictureService pictureService;

    /**
     * 校验空间分析权限
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        // 仅管理员可以查询全空间以及公共图库
        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else {
            // 校验空间id参数
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            // 仅管理员和创建者可以查询指定空间
            ThrowUtils.throwIf(!userService.isAdmin(loginUser) && !space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);

        }
    }

    /**
     * 填充分析查询条件
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        if (queryAll) {
            // 全空间分析，无需添加条件
            return;
        } else if (queryPublic) {
            // 查询公共图库
            queryWrapper.isNull("spaceId");
            return;
        } else {
            // 查询指定空间
            if (spaceId != null) {
                queryWrapper.eq("spaceId", spaceId);
                return;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定任何查询范围");
    }

    @Override
    public SpaceUsageAnalyzeResponse spaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {

        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);

        // 校验权限
        checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);

        // 判断是查询全部空间、公共图库还是指定空间
        if (spaceUsageAnalyzeRequest.isQueryPublic() || spaceUsageAnalyzeRequest.isQueryAll()) {

            // 仅管理员可以查询全空间以及公共图库
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            if (!spaceUsageAnalyzeRequest.isQueryAll()) {
                // 查询公共图库
                queryWrapper.isNull("spaceId");
            }
            // 查询到所有图片大小
            List<Object> objects = this.baseMapper.selectObjs(queryWrapper);
            // 总大小
            long sum = objects.stream().mapToLong(result -> (Long) result).sum();
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(sum);
            spaceUsageAnalyzeResponse.setMaxSize(null); // 全空间或公共图库无大小限制
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setUsedCount((long) objects.size());
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;

        } else {
            // 查询指定空间
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            Space space = spaceService.getById(spaceId);
            Long maxSize = space.getMaxSize();
            Long maxCount = space.getMaxCount();
            Long totalSize = space.getTotalSize();
            Long totalCount = space.getTotalCount();

            // 后端直接计算使用比例，避免前端计算误差
            double sizeUsageRatio = NumberUtil.round(totalSize * 100.0 / maxSize, 2).doubleValue();
            double countUsageRatio = NumberUtil.round(totalCount * 100.0 / maxCount, 2).doubleValue();

            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(totalSize);
            spaceUsageAnalyzeResponse.setMaxSize(maxSize);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setUsedCount(totalCount);
            spaceUsageAnalyzeResponse.setMaxCount(maxCount);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);

            return spaceUsageAnalyzeResponse;

        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {

        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);

        // 校验权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 填充查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        // 使用MyBatis-Plus的分组查询
        queryWrapper.select("category", "COUNT(*) AS count", "SUM(picSize) AS totalSize");
        queryWrapper.groupBy("category");
        List<Map<String, Object>> maps = this.baseMapper.selectMaps(queryWrapper);

        // 转换为响应对象列表
        return maps.stream().map(result -> {
            String category = result.get("category") != null ? (String) result.get("category") : "未分类";
            Long count = ((Number) result.get("count")).longValue();
            Long totalSize = ((Number) result.get("totalSize")).longValue();
            return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
        }).collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> spaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {

        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);
        // 校验权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 填充查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);

        queryWrapper.select("tags");
        List<Object> objects = this.baseMapper.selectObjs(queryWrapper);
        // 合并标签，并统计出现次数
        Map<String, Long> collect = objects.stream().filter(ObjUtil::isNotNull).flatMap(result ->
                    //将json字符串转换为List<String>并扁平化
                    JSONUtil.toList(JSONUtil.toJsonStr(result), String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting())); // 统计每个标签出现的次数

        return collect.entrySet().stream()
                .sorted((a,b) -> Long.compare(b.getValue(), a.getValue())) // 按照标签出现次数降序排序
                .map(entry -> {
            String tag = entry.getKey();
            Long count = entry.getValue();
            return new SpaceTagAnalyzeResponse(tag, count);
        }).collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> spaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);
        // 校验权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 填充查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        queryWrapper.select("picSize");
        // 获取到所有图片大小
        List<Object> objects = this.baseMapper.selectObjs(queryWrapper);

        // 定义分段范围，使用有序(先后顺序)Map存储结果
        Map<String,Long> map = new LinkedHashMap<>();
        map.put("<100KB",objects.stream().filter(result -> ((Long)result) < 100 * 1024).count());
        map.put("100KB-500KB",objects.stream().filter(result -> ((Long)result) < 500 * 1024 && ((Long)result) >= 100 * 1024).count());
        map.put("500KB-1MB",objects.stream().filter(result -> ((Long)result) < 1 * 1024 * 1024 && ((Long)result) >= 500 * 1024).count());
        map.put(">1MB",objects.stream().filter(result -> ((Long)result) >= 1 * 1024 * 1024).count());
        // 转换为响应对象列表
        return map.entrySet().stream().map(entry -> {;
            String sizeRange = entry.getKey();
            Long count = entry.getValue();
            return new SpaceSizeAnalyzeResponse(sizeRange, count);
        }).collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> spaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {

        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);
        // 校验权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 填充查询条件
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId),"userId",userId);

        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度" );
        }

        queryWrapper.groupBy("period").orderByAsc("period");
        List<Map<String, Object>> maps = this.baseMapper.selectMaps(queryWrapper);
        // 转换为响应对象列表
        return maps.stream().map(result -> {
            String period = String.valueOf(result.get("period"));
            Long count = (Long) result.get("count");
            return new SpaceUserAnalyzeResponse(period, count);
        }).collect(Collectors.toList());

    }

    @Override
    public List<Space> spaceRankingAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);
        // 仅管理员可以进行空间使用排名分析
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        // 仅查询必要字段，提高性能
        queryWrapper.select("id","spaceName","totalSize","userId")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN());

        return spaceService.list(queryWrapper);
    }
}




