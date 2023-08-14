package com.cache;

import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.cache.annotation.SimpleCache;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

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
        //只缓存某一个些参数,或者除了某些参数都缓存
        Predicate<Request> requestPredicate = onlyParamKeyByRequestToCache();
        if (requestPredicate != null) {
            if (onlyParamKeyByRequestToCache(request, requestPredicate)) {
                publishJob(request,result,targetObject,targetMethod,annotation,key);
                return;
            }
        }
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
            Request originalRequest = (Request)DeepCopy.copy(request);
            Request changeRequest = paramFunctionKey.apply(originalRequest);
            easyReadRedisData = paramKeyByRequest(changeRequest);
        }else {
            easyReadRedisData = paramKeyByRequest(request);
        }
//        String jsonString = targetMethod.getDeclaringClass() + "." +targetMethod.getName() + "#" + easyReadRedisData;
        String jsonString = getAbbreviatedName(targetMethod) + ":" + easyReadRedisData;
        if(StringUtils.isEmpty(jsonString)){
            throw new IllegalArgumentException("缓存参数错误");
        }
        return jsonString;
    }

    public static String getAbbreviatedName(Method method) {
        String fullName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        String[] nameParts = fullName.split("\\.");

        // Build the abbreviated name
        StringBuilder abbreviatedName = new StringBuilder();
        for (int i = 0; i < nameParts.length - 1; i++) {
            abbreviatedName.append(nameParts[i].charAt(0)).append(".");
        }
        abbreviatedName.append(nameParts[nameParts.length - 1]);

        return abbreviatedName + ":" + methodName;
    }


    @Override
    public String paramKeyByRequest(Request request) {
        //空字符串不作为参数
        PropertyFilter filter = (source, name, value) -> !(value instanceof String) ||
                !((String) value).isEmpty();

        String requestString = JSON.toJSONString(request,filter);
        return requestString.replace(":", "$");
    }

    @Override
    public boolean onlyParamKeyByRequestToCache(Request request, Predicate<Request> predicate) {
        return predicate.test(request);
    }

    public Predicate<Request> onlyParamKeyByRequestToCache(){
        return null;
    }


}
