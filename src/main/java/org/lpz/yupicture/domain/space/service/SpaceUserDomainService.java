package org.lpz.yupicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lpz.yupicture.domain.space.entity.SpaceUser;
import org.lpz.yupicture.interfaces.dto.space.spaceuser.SpaceUserAddRequest;
import org.lpz.yupicture.interfaces.dto.space.spaceuser.SpaceUserQueryRequest;
import org.lpz.yupicture.interfaces.vo.space.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author lpz
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-12-13 21:37:04
*/
public interface SpaceUserDomainService extends IService<SpaceUser> {


    /**
     * 将查询请求转换为QueryWrapper对象
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

}
