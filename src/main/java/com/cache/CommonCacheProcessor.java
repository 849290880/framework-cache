package com.cache;

import com.alibaba.fastjson.JSON;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public class CommonCacheProcessor<Request,Response> extends CacheProcessorAbstract<Request,Response>{

    public final static String COMMON_CACHE_KEY = "COMMON_CACHE_KEY";
    @Override
    public Response returnCacheResult(Request request,SimpleCache annotation,Method targetMethod) {

        //这里扩展点为 paramKeyByRequest方法的重写,根据不同的参数定义不同的缓存key
        String finalKey = generateKey(request, annotation, targetMethod);

        //这里扩展点为
        Response json = returnResult(finalKey);

        //记录缓存任务中最后缓存命中的时间
        RefreshCache refreshCache = CacheJob.refreshCacheMap.get(finalKey);
        if(annotation.addToJob() && refreshCache!=null){
            refreshCache.getCacheCount().incrementAndGet();
            refreshCache.saveLastHitTime();
        }

        return json;
    }


    public Response returnResult(String finalKey) {
        return (Response)redisTemplate.opsForValue().get(finalKey);
    }

    @Override
    public String generateKey(Request request, SimpleCache annotation, Method targetMethod) {
        String prefixKey = annotation.prefixKey();
        return generateCacheKey(request, targetMethod, prefixKey);
    }

    @Override
    public String generateCacheKey(Request request, Method targetMethod, String prefixKey) {
        if(StringUtils.isEmpty(prefixKey)){
            prefixKey = COMMON_CACHE_KEY;
        }
        //序列化request,将request的:替换
        String queryKey = paramKey(targetMethod, request);
        return prefixKey + ":" + queryKey;
    }

    @Override
    public void putCacheResult(Request request, Object result, Object targetObject,
                               Method targetMethod,SimpleCache annotation) {
        String key = generateKey(request, annotation, targetMethod);
        if (annotation.addToJob()) {
            addToCacheJob(request, targetObject, targetMethod,annotation,key);
        }
        putToCache(request,result,key,annotation.cacheTime(),annotation.timeUnit());
    }

    private void addToCacheJob(Request request, Object targetObject, Method method, SimpleCache annotation,String key) {
        //加入缓存定时任务
        RefreshCache refreshCache = new RefreshCache(method, targetObject, request,
                key, this,
                annotation.cron(),(int)annotation.fixTime(),
                System.currentTimeMillis(), annotation.cacheTime(),annotation.timeUnit(),annotation.ttlTime());
        CacheJob.refreshCacheMap.put(key,refreshCache);
    }

    @Override
    public void putToCache(Request request, Object result,String key,long timeout,TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key,result,timeout,timeUnit);
    }

    @Override
    public void removeCache(String key){
        redisTemplate.delete(key);
    }

    private String paramKey(Method targetMethod, Request request){
        //替换 : 为特殊符号
        String easyReadRedisData = paramKeyByRequest(request);
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
