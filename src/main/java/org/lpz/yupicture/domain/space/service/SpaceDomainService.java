package org.lpz.yupicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lpz.yupicture.domain.space.entity.Space;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.interfaces.dto.space.SpaceAddRequest;
import org.lpz.yupicture.interfaces.dto.space.SpaceQueryRequest;
import org.lpz.yupicture.interfaces.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author lpz
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-11-16 16:57:24
*/
public interface SpaceDomainService extends IService<Space> {


    /**
     * 将查询请求转换为QueryWrapper对象
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


    /**
     * 根据空间等级填充空间数据
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

}
