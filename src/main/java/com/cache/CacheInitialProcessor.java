package com.cache;

import com.cache.annotation.CacheInitial;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.lang.reflect.Method;

public interface CacheInitialProcessor<Request,Response>{

    void init(CacheInitial cacheInitial, Method method, Object target);

    void initCacheTool(RedisTemplate<String, Object> redisTemplate);

    void initThreadPool(ThreadPoolTaskScheduler threadPoolTaskScheduler);

    void refresh();

    void initPublisher(EventPublisher eventPublisher);

    void init();
}
