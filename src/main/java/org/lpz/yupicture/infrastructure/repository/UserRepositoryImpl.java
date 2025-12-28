package org.lpz.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.domain.user.repository.UserRepository;
import org.lpz.yupicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}
