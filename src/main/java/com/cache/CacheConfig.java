package com.cache;

import com.cache.aspect.CacheAspect;
import com.cache.listener.CacheEventListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

public class CacheConfig {

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContextProvider applicationContextProvider(){
        return new ApplicationContextProvider();
    }

    @Bean
    public CacheInitialAnnotationBeanPostProcessor cacheInitialAnnotationBeanPostProcessor() {
        return new CacheInitialAnnotationBeanPostProcessor();
    }

    @Bean(name = "cacheScheduler")
    @ConditionalOnMissingBean(name = "cacheScheduler")
    public TaskScheduler threadPoolTaskScheduler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(3);
        threadPoolTaskScheduler.setThreadNamePrefix("x-cache-task");
        threadPoolTaskScheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }

    @Bean(name = "cacheTemplate")
    @ConditionalOnMissingBean(name = "cacheTemplate")
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

    @Bean(name = "eventPublisher")
    @ConditionalOnMissingBean
    public EventPublisher eventPublisher(){
        return new EventPublisher();
    }


    @Bean
    @ConditionalOnMissingBean
    public CacheAspect cacheAspect(@Qualifier("cacheTemplate") RedisTemplate<String,Object> cacheTemplate,
                                   EventPublisher eventPublisher){
        return new CacheAspect(cacheTemplate,eventPublisher);
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
    public CacheEventListener cacheJob(){
        return new CacheEventListener();
    }



}

