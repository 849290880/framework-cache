package com.cache;

import com.cache.annotation.SimpleCache;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 使用组合的方法将两个注解的功能融合一起 @CacheInitial @SimpleCache
 * @param <Request>
 * @param <Response>
 */
public class SimpleCacheInitialProcessor<Request,Response> extends CacheInitialProcessorAbstract<Request,Response> implements SimpleCacheProcessor<Request,Response>{

    private final CacheProcessorAbstract<Request,Response> cacheProcessorAbstract;

    public SimpleCacheInitialProcessor(){
        cacheProcessorAbstract = new CommonCacheProcessor<>();
    }

    @Override
    public void init() {
        this.cacheProcessorAbstract.buildCacheProvide(this.redisTemplate);
        this.cacheProcessorAbstract.buildEventPublisher(eventPublisher);
        this.request = initialRequestParam();
        this.key = initialKey();
    }

    @Override
    public Response returnCacheResult(Request request, SimpleCache annotation, Method targetMethod) {
        String cacheKey = cacheProcessorAbstract.generateKey(request, annotation, targetMethod);

        //发布缓存任务事件
        if(annotation.addToJob()){
            eventPublisher.publishRefreshJobEvent(cacheKey);
        }

        return (Response) redisTemplate.opsForValue().get(cacheKey);
    }

    @Override
    public void putCacheResult(Request request, Response result, Object targetObject, Method targetMethod, SimpleCache simpleCache) {
        cacheProcessorAbstract.putCacheResult(request,result,targetObject,targetMethod,simpleCache);
    }


    @Override
    public void buildEventPublisher(EventPublisher eventPublisher) {
        if(this.eventPublisher == null){
            this.eventPublisher = eventPublisher;
        }
        this.cacheProcessorAbstract.buildEventPublisher(eventPublisher);
    }

    @Override
    public void putToCache(Request request, Response result, String key, long timeout, TimeUnit timeUnit) {
        cacheProcessorAbstract.putToCache(request,result,key,timeout,timeUnit);
    }

    @Override
    public void removeCache(String key) {
        cacheProcessorAbstract.removeCache(key);
    }

    @Override
    public void buildCacheProvide(Object cacheProvide) {
        if (this.redisTemplate==null) {
            this.redisTemplate = (RedisTemplate<String, Object>) cacheProvide;
        }
        if(cacheProcessorAbstract.getRedisTemplate() == null){
            cacheProcessorAbstract.buildCacheProvide(cacheProvide);
        }
    }



    /**
     * 当请求进来的时候,根据参数定义缓存的key是什么
     *
     * @param request
     * @return
     */
    @Override
    public String paramKeyByRequest(Request request) {
        return cacheProcessorAbstract.paramKeyByRequest(request);
    }


    /**
     * 初始化缓存的时候,调用方法时的参数
     */
    @Override
    public Request initialRequestParam() {
        return null;
    }

    @Override
    public String initialKey() {
        return cacheProcessorAbstract.generateCacheKey(request,method,cacheInitial.prefixKey());
    }


    /**
     * 将结果保存到缓存
     */
    @Override
    public void saveToCache(Request request,Response result,long time,TimeUnit timeUnit,
                            Method targetMethod,String prefix) {
        String cacheKey = cacheProcessorAbstract.generateCacheKey(request,targetMethod,prefix);
        redisTemplate.opsForValue().set(cacheKey,result,time,timeUnit);
    }

    @Override
    public void deleteCache(Request request,Method targetMethod,String prefix) {
        String cacheKey = cacheProcessorAbstract.generateCacheKey(request,targetMethod,prefix);
        redisTemplate.delete(cacheKey);
    }

    /**
     * 刷新缓存的方法
     */
    @Override
    public void refresh() {
        super.refresh();
    }
}
