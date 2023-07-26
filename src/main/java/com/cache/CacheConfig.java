package com.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class CacheConfig {

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContextProvider applicationContextProvider(){
        return new ApplicationContextProvider();
    }

    @Bean(name = "cacheTemplate")
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> cacheTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);

        FastJsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJsonRedisSerializer<>(Object.class);

        // 使用 String 序列化器作为 key 的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // 使用 JDK 序列化器作为 value 的序列化器
        redisTemplate.setValueSerializer(fastJsonRedisSerializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);
        return redisTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheAspect cacheAspect(@Qualifier("cacheTemplate") RedisTemplate<String,Object> cacheTemplate){
        return new CacheAspect(cacheTemplate);
    }

    @Value("${my.cron.expression:*/5 * * * * *}")
    private String cronExpressionValue;

    @Bean
    @ConditionalOnMissingBean
    public CronConfig cronConfig(){
        return new CronConfig(cronExpressionValue);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheJob cacheJob(){
        return new CacheJob();
    }



}

