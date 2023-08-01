package com.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

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
    @ConditionalOnMissingBean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(3);
        threadPoolTaskScheduler.setThreadNamePrefix("init-cache-job");
        threadPoolTaskScheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
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

