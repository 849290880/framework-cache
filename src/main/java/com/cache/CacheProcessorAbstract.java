package com.cache;

import org.springframework.data.redis.core.RedisTemplate;

public abstract class CacheProcessorAbstract<Request,Response> implements CacheProcessor<Request,Response>{

    protected RedisTemplate<String, Object> redisTemplate;

    @Override
    public void buildCacheProvide(Object cacheProvide){
        redisTemplate = (RedisTemplate<String, Object>) cacheProvide;
    }

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }
}
