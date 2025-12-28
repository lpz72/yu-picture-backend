package org.lpz.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lpz.yupicture.domain.picture.entity.Picture;
import org.lpz.yupicture.domain.picture.reposity.PictureRepository;
import org.lpz.yupicture.domain.user.entity.User;
import org.lpz.yupicture.domain.user.repository.UserRepository;
import org.lpz.yupicture.infrastructure.mapper.PictureMapper;
import org.lpz.yupicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {

}
