package com.cache;

import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;

public abstract class CacheProcessorAbstract<Request,Response> implements CacheProcessor<Request,Response>{

    protected RedisTemplate<String, Object> redisTemplate;

    @Override
    public void buildCacheProvide(Object cacheProvide){
        redisTemplate = (RedisTemplate<String, Object>) cacheProvide;
    }

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }


    public abstract String generateKey(Request request, SimpleCache annotation, Method targetMethod);

    public abstract String generateCacheKey(Request request, Method targetMethod, String prefixKey);
}
