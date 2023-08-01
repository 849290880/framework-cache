package com.cache;

import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;

public abstract class CacheInitialProcessorAbstract<Request,Response> {

    protected RedisTemplate<String, Object> redisTemplate;

    protected CacheInitial cacheInitial;

    protected Method method;

    protected Object bean;

    public void init(CacheInitial cacheInitial, Method method, Object bean){
        this.cacheInitial = cacheInitial;
        this.method = method;
        this.bean = bean;
    }
    public abstract Request initialRequestParam();

    public abstract void saveToCache(Response result);

    public void initCacheTool(RedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    public CacheInitial getCacheInitial() {
        return cacheInitial;
    }

    public Method getMethod() {
        return method;
    }

    public Object getBean() {
        return bean;
    }

    //    public abstract void timeToSaveCache(Request param, Method method, Object bean);
}
