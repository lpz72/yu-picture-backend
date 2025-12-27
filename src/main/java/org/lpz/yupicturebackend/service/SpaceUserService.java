package org.lpz.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.lpz.yupicturebackend.model.dto.space.SpaceAddRequest;
import org.lpz.yupicturebackend.model.dto.space.SpaceQueryRequest;
import org.lpz.yupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import org.lpz.yupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import org.lpz.yupicturebackend.model.entity.Space;
import org.lpz.yupicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.model.vo.SpaceUserVO;
import org.lpz.yupicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author lpz
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-12-13 21:37:04
*/
public interface SpaceUserService extends IService<SpaceUser> {


    /**
     * 将查询请求转换为QueryWrapper对象
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取空间成员封装类，并关联用户信息
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);


    /**
     * 空间成员数据校验
     * @param spaceUser
     */
    void validSpaceUser(SpaceUser spaceUser,boolean add);


    /**
     * 创建空间成员
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 获取空间封装列表
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

}
