package com.cache;

import com.cache.annotation.CacheInitial;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class CacheInitialProcessorAbstract<Request,Response> implements CacheInitialProcessor<Request,Response>,CacheProcessor<Request,Response>{

    protected RedisTemplate<String, Object> redisTemplate;

    protected CacheInitial cacheInitial;

    protected Method method;

    protected Object bean;
    protected ThreadPoolTaskScheduler threadPoolTaskScheduler;

    protected volatile boolean initFlag;

//    protected Request request;
    protected List<Request> requestList;

    protected Map<String,Request> cacheKeyRequestMap;


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
        this.requestList = initRequest();
        this.cacheKeyRequestMap = initialCacheKeyMap(requestList);
    }

    public abstract List<Request> initialRequestParam();

    public List<Request> initRequest(){
        List<Request> requests = new ArrayList<>();
        List<Request> customRequest = initialRequestParam();
        if(customRequest != null){
            requests.addAll(customRequest);
        }
        return requests;
    }

    public abstract String initialKey(Request request);

    public Map<String,Request> initialCacheKeyMap(List<Request> requestList){
        Map<String,Request> cacheKeyMap = new HashMap<>();
        if(CollectionUtils.isEmpty(requestList)){
            return cacheKeyMap;
        }
        for (Request request : requestList) {
            String cacheKey = initialKey(request);
            cacheKeyMap.put(cacheKey,request);
        }
        return cacheKeyMap;
    }

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

        for (Request request : this.requestList) {
            String key = initialKey(request);
            //发布定时缓存任务
            RefreshCache refreshCache = new RefreshCache(method, bean, request,
                    key, this,
                    cacheInitial.cron(),(int)cacheInitial.fixTime(),
                    System.currentTimeMillis(), cacheInitial.cacheTime(),cacheInitial.timeUnit(),cacheInitial.ttlTime());

            //发布缓存刷新任务
            eventPublisher.publishAddJobEvent(refreshCache,key);
        }

    }


}
