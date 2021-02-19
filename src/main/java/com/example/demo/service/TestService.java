package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void saveApi() {
        System.out.println(">>>>>>>>>>>>>>");
        for (int i = 0; i < 10; i++) {
            System.out.println(">>>>>>>>save>>>>>>" + i);
            redisTemplate.opsForValue().set("key" + i, "value" + i);
            System.out.println(">>>>>>>>after>>>>>>" + i);

        }
    }

    public Object getRedis(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }
}
