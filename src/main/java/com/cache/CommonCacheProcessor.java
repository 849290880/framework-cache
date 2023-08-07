package com.cache;

import com.alibaba.fastjson.JSON;
import com.cache.annotation.SimpleCache;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CommonCacheProcessor<Request,Response> extends CacheProcessorAbstract<Request,Response>{

    public final static String COMMON_CACHE_KEY = "CACHE_KEY";
    @Override
    public Response returnCacheResult(Request request, SimpleCache annotation, Method targetMethod) {

        //这里扩展点为 paramKeyByRequest方法的重写,根据不同的参数定义不同的缓存key
        String cacheKey = generateKey(request, annotation, targetMethod);

        //这里扩展点为
        Response json = returnResult(cacheKey);

        //发布缓存任务事件
        if(annotation.addToJob()){
            eventPublisher.publishRefreshJobEvent(cacheKey);
        }

        return json;
    }

    @Override
    public void putCacheResult(Request request, Response result, Object targetObject, Method targetMethod, SimpleCache annotation) {
        String key = generateKey(request, annotation, targetMethod);
        publishJob(request,result,targetObject,targetMethod,annotation,key);
    }

    @Override
    public void publishJob(Request request, Response result, Object targetObject, Method targetMethod, SimpleCache annotation, String key) {
        if (annotation.addToJob()) {
            addToCacheJob(request, targetObject, targetMethod,annotation,key);
        }
        putToCache(request,result,key,annotation.cacheTime(),annotation.timeUnit());
    }

    public Response returnResult(String finalKey) {
        return (Response)redisTemplate.opsForValue().get(finalKey);
    }

    @Override
    public String generateKey(Request request, SimpleCache annotation, Method targetMethod) {
        String prefixKey = annotation.prefixKey();
        return generateCacheKey(request, targetMethod, prefixKey,null);
    }

    @Override
    public String generateCacheKey(Request request, Method targetMethod, String prefixKey,
                                   Function<Request,Request> paramFunctionKey) {
        if(StringUtils.isEmpty(prefixKey)){
            prefixKey = COMMON_CACHE_KEY;
        }
        //序列化request,将request的:替换
        String queryKey = paramKey(targetMethod, request,paramFunctionKey);
        return prefixKey + ":" + queryKey;
    }


    public void addToCacheJob(Request request, Object targetObject, Method method, SimpleCache annotation,String key) {
        //加入缓存定时任务
        RefreshCache refreshCache = new RefreshCache(method, targetObject, request,
                key, this,
                annotation.cron(),(int)annotation.fixTime(),
                System.currentTimeMillis(), annotation.cacheTime(),annotation.timeUnit(),annotation.ttlTime());

        //发布缓存刷新任务
        eventPublisher.publishAddJobEvent(refreshCache,key);
    }

    @Override
    public void putToCache(Request request, Response result, String key, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key,result,timeout,timeUnit);
    }

    @Override
    public void removeCache(String key){
        redisTemplate.delete(key);
    }

    public String paramKey(Method targetMethod, Request request, Function<Request,Request> paramFunctionKey){
        //替换 : 为特殊符号
        String easyReadRedisData = null;
        if(paramFunctionKey != null){
            Request changeRequest = paramFunctionKey.apply(request);
            easyReadRedisData = paramKeyByRequest(changeRequest);
        }else {
            easyReadRedisData = paramKeyByRequest(request);
        }
        String jsonString = targetMethod.getDeclaringClass() + "." +targetMethod.getName() + "#" + easyReadRedisData;
        if(StringUtils.isEmpty(jsonString)){
            throw new IllegalArgumentException("缓存参数错误");
        }
        return jsonString;
    }

    @Override
    public String paramKeyByRequest(Request request) {
        String requestString = JSON.toJSONString(request);
        return requestString.replace(":", "$");
    }


}
