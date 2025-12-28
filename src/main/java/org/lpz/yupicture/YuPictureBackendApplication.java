package org.lpz.yupicture;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@MapperScan("org.lpz.yupicture.infrastructure.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
public class YuPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuPictureBackendApplication.class, args);
    }

}
