package org.lpz.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lpz.yupicture.domain.space.entity.Space;
import org.lpz.yupicture.domain.space.repository.SpaceRepository;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.domain.user.repository.UserRepository;
import org.lpz.yupicture.infrastructure.mapper.SpaceMapper;
import org.lpz.yupicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceRepositoryImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceRepository {
}
