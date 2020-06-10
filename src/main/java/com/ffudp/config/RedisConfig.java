package com.ffudp.config;

import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import com.ffudp.listener.ParsingListener;
import com.ffudp.listener.ParsingNewListener;
import com.ffudp.listener.SaveDBListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author  lm
 * @create 2019-12-27 9:53
 * @desc
 **/

@Configuration
public class RedisConfig {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    @Autowired
    private ParsingListener pListener;
    @Autowired
    private ParsingNewListener pNListener;
    @Autowired
    private SaveDBListener sListener;


    /**
     * 重写Redis序列化方式，使用Json方式:
     * 当我们的数据存储到Redis的时候，我们的键（key）和值（value）都是通过Spring提供的Serializer序列化到数据库的。RedisTemplate默认使用的是JdkSerializationRedisSerializer，StringRedisTemplate默认使用的是StringRedisSerializer。
     * Spring Data JPA为我们提供了下面的Serializer：
     * GenericToStringSerializer、Jackson2JsonRedisSerializer、JacksonJsonRedisSerializer、JdkSerializationRedisSerializer、OxmSerializer、StringRedisSerializer。
     * 在此我们将自己配置RedisTemplate并定义Serializer。
     *
     * @param redisConnectionFactory
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        FastJsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJsonRedisSerializer<>(Object.class);
        // 全局开启AutoType，不建议使用
        // ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        // 建议使用这种方式，小范围指定白名单
        ParserConfig.getGlobalInstance().addAccept("com.");

        // 设置值（value）的序列化采用FastJsonRedisSerializer。
        redisTemplate.setValueSerializer(fastJsonRedisSerializer);
        redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);
        // 设置键（key）的序列化采用StringRedisSerializer。
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }



    /**
     * @author 七脉 描述：需要手动定义RedisMessageListenerContainer加入IOC容器
     * @return
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        container.setConnectionFactory(redisConnectionFactory);
        /**
         * 添加订阅者监听类，数量不限.PatternTopic定义监听主题,这里监听dj主题
         */
//        container.addMessageListener(new SubscribeListener(), new PatternTopic("dj"));
        container.addMessageListener(pListener, new PatternTopic("Parsing"));
        container.addMessageListener(pNListener, new PatternTopic("Parsing2"));
        container.addMessageListener(sListener, new PatternTopic("Save"));
        return container;

    }
}
