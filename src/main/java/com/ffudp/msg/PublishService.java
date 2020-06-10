package com.ffudp.msg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author: 729002330@qq.com
 * @date :2020-05-29
 */
@Component
public class PublishService {
    @Autowired
    RedisTemplate redisTemplate;
    /**
     * 发布方法
     * @param channel 消息发布订阅 主题
     * @param message 消息信息
     */
    public void publish(String channel, Object message) {
        // 该方法封装的 connection.publish(rawChannel, rawMessage);
        redisTemplate.convertAndSend(channel, message);
    }
}
