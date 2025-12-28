package org.lpz.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lpz.yupicture.domain.space.entity.Space;
import org.lpz.yupicture.domain.space.entity.SpaceUser;
import org.lpz.yupicture.domain.space.repository.SpaceRepository;
import org.lpz.yupicture.domain.space.repository.SpaceUserRepository;
import org.lpz.yupicture.infrastructure.mapper.SpaceMapper;
import org.lpz.yupicture.infrastructure.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserRepository {
}
