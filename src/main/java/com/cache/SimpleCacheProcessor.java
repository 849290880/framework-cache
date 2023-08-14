package com.cache;


import com.cache.annotation.SimpleCache;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public interface SimpleCacheProcessor<Request,Response>{

//    Response returnCacheResult(Request request,Class<?> clazz);

    Response returnCacheResult(Request request, SimpleCache simpleCache, Method targetMethod);
    void putCacheResult(Request request, Response result, Object targetObject, Method targetMethod,SimpleCache simpleCache);


    void buildCacheProvide(Object cacheProvide);

    void buildEventPublisher(EventPublisher eventPublisher);

    String paramKeyByRequest(Request request);

    /**
     * 当这个参数计算出来的key等于某一个值，才进行缓存
     * @param request
     * @param predicate
     * @return
     */
    boolean onlyParamKeyByRequestToCache(Request request, Predicate<Request> predicate);


}
