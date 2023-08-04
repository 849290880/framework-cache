package com.cache;

import com.cache.annotation.CacheInitial;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public abstract class CacheInitialProcessorAbstract<Request,Response> implements CacheInitialProcessor<Request,Response>,CacheProcessor<Request,Response>{

    protected RedisTemplate<String, Object> redisTemplate;

    protected CacheInitial cacheInitial;

    protected Method method;

    protected Object bean;
    protected ThreadPoolTaskScheduler threadPoolTaskScheduler;

    protected volatile boolean initFlag;

    protected Request request;

    protected String key;

    protected EventPublisher eventPublisher;

    @Override
    public void init(CacheInitial cacheInitial, Method method, Object bean){
        this.cacheInitial = cacheInitial;
        this.method = method;
        this.bean = bean;
        this.initFlag = true;
    }

    @Override
    public void init() {
        this.request = initialRequestParam();
        this.key = initialKey();
    }

    public abstract Request initialRequestParam();

    public abstract String initialKey();

    @Override
    public void removeCache(String key) {

    }

    @Override
    public void putToCache(Request request, Response result, String key, long timeout, TimeUnit timeUnit) {
        saveToCache(request,result,timeout,timeUnit,method, cacheInitial.prefixKey());
    }

    public abstract void saveToCache(Request request, Response result, long time,
                                     TimeUnit timeUnit, Method method, String prefix);

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
    public void initPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void refresh() {

        //发布定时缓存任务
        RefreshCache refreshCache = new RefreshCache(method, bean, request,
                key, this,
                cacheInitial.cron(),(int)cacheInitial.fixTime(),
                System.currentTimeMillis(), cacheInitial.cacheTime(),cacheInitial.timeUnit(),cacheInitial.ttlTime());

        //发布缓存刷新任务
        eventPublisher.publishAddJobEvent(refreshCache,key);

//        threadPoolTaskScheduler.schedule(new Runnable() {
//            @Override
//            public void run() {
//                Object request = initialRequestParam();
//                //启动删除原来缓存的数据
//                if (cacheInitial.deletePreviousKey() && initFlag) {
//                    synchronized (this){
//                        deleteCache((Request) request,method,cacheInitial.prefixKey());
//                        initFlag = false;
//                    }
//                }
//
//                Object result = null;
//                try {
//                    if(request == null){
//                        result = method.invoke(bean);
//                    }else {
//                        result = method.invoke(bean, request);
//                    }
//                } catch (IllegalAccessException | InvocationTargetException e) {
//                    throw new RuntimeException(e);
//                }
//                saveToCache((Request) request,(Response) result,
//                        cacheInitial.cacheTime(),cacheInitial.timeUnit(),method,cacheInitial.prefixKey());
//            }
//        },new CronTrigger(getCacheInitial().cron()));
    }


}
