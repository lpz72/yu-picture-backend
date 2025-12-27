package org.lpz.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.lpz.yupicturebackend.model.dto.picture.PictureQueryRequest;
import org.lpz.yupicturebackend.model.dto.space.SpaceAddRequest;
import org.lpz.yupicturebackend.model.dto.space.SpaceQueryRequest;
import org.lpz.yupicturebackend.model.entity.Picture;
import org.lpz.yupicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.vo.PictureVO;
import org.lpz.yupicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author lpz
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-11-16 16:57:24
*/
public interface SpaceService extends IService<Space> {


    /**
     * 将查询请求转换为QueryWrapper对象
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取空间封装类，并关联用户信息
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 分页获取空间封装
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 空间数据校验
     * @param space
     */
    void validSpace(Space space,boolean add);

    /**
     * 根据空间等级填充空间数据
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param user
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User user);



}
