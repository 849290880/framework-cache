package com.cache;

import com.cache.annotation.CacheInitial;
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


    @Override
    public void init(CacheInitial cacheInitial, Method method, Object bean){
        this.cacheInitial = cacheInitial;
        this.method = method;
        this.bean = bean;
        this.initFlag = true;
    }
    public abstract Request initialRequestParam();

    public abstract void saveToCache(Request request, Response result, long time,
                                     TimeUnit timeUnit,Method method,String prefix);

    public void deleteCache(Request request,Method method,String prefixKey){

    }

    @Override
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

    @Override
    public void initThreadPool(ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
    }

    @Override
    public void refresh() {
        threadPoolTaskScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Object request = initialRequestParam();
                //启动删除原来缓存的数据
                if (cacheInitial.deletePreviousKey() && initFlag) {
                    synchronized (this){
                        deleteCache((Request) request,method,cacheInitial.prefixKey());
                        initFlag = false;
                    }
                }

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
