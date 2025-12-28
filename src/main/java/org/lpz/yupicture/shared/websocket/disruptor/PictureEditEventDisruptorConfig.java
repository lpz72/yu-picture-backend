package org.lpz.yupicture.shared.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;


    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingModel() {

        // ringBuffer大小
        int size = 1024 * 256;
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(PictureEditEvent::new, size, ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build());

        // 设置消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);

        // 启动
        disruptor.start();

        return disruptor;

    }

}
