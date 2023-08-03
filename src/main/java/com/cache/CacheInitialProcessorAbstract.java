package com.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public abstract class CacheInitialProcessorAbstract<Request,Response> implements CacheInitialProcessor<Request,Response>{

    protected RedisTemplate<String, Object> redisTemplate;

    protected CacheInitial cacheInitial;

    protected Method method;

    protected Object bean;
    protected ThreadPoolTaskScheduler threadPoolTaskScheduler;

    protected volatile boolean initFlag;

    public void init(CacheInitial cacheInitial, Method method, Object bean){
        this.cacheInitial = cacheInitial;
        this.method = method;
        this.bean = bean;
        this.initFlag = false;
    }
    public abstract Request initialRequestParam();

    public abstract void saveToCache(Request request, Response result, long time,
                                     TimeUnit timeUnit,Method method,String prefix);

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

    public void initThreadPool(ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
    }

    public void refresh() {
        threadPoolTaskScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Object request = initialRequestParam();
                Object result = null;
                try {
                    if(request == null){
                        result = method.invoke(bean);
                    }else {
                        result = method.invoke(bean, request);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                saveToCache((Request) request,(Response) result,
                        cacheInitial.cacheTime(),cacheInitial.timeUnit(),method,cacheInitial.prefixKey());
            }
        },new CronTrigger(getCacheInitial().cron()));
    }


}
