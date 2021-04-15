package com.ffudp.test;

import com.alibaba.fastjson.JSONObject;
import com.ffudp.dbo.ObTaskB;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;


public class test {
    @Resource
    @Autowired
    @Lazy
    private static RedisTemplate redisTemplate;
    public static void main(String[] args) {
        Object key0 = "6001";
        try {
            JSONObject info0 = (JSONObject)redisTemplate.opsForValue().get(key0);
            if(info0 != null) {
                System.out.println(info0);
                System.out.println(info0.toJSONString());
                ObTaskB tskB = JSONObject.parseObject(info0.toJSONString(), ObTaskB.class);
            }else{
                System.out.println("无数据");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
