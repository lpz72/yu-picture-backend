package org.lpz.yupicturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisStringTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testStringRedis() {
        // 获取操作对象
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();

        // key 和 value
        String key = "testKey";
        String value = "testValue";

        // 1. 测试新增或更新操作
        opsForValue.set(key, value);
        String s = opsForValue.get(key);
        assertEquals(value, s,"存储的值与预期不一致");

        // 2. 测试修改操作
        String testUpdate = "testUpdate";
        opsForValue.set(key, testUpdate);
        String updatedValue = opsForValue.get(key);
        assertEquals(testUpdate, updatedValue,"更新的值与预期不一致");

        // 3.测试查询操作
        String fetchedValue = opsForValue.get(key);
        assertNotNull(fetchedValue,"查询值为空");

        // 4.测试删除操作
        stringRedisTemplate.delete(key);
        String deletedValue = opsForValue.get(key);
        assertNull(deletedValue,"删除后值仍然存在");
    }

}
