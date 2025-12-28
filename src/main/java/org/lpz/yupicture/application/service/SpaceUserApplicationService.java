package org.lpz.yupicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.lpz.yupicture.interfaces.dto.space.spaceuser.SpaceUserAddRequest;
import org.lpz.yupicture.interfaces.dto.space.spaceuser.SpaceUserQueryRequest;
import org.lpz.yupicture.domain.space.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lpz.yupicture.interfaces.vo.space.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author lpz
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-12-13 21:37:04
*/
public interface SpaceUserApplicationService extends IService<SpaceUser> {


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
