package com.cache;


import com.cache.annotation.SimpleCache;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public interface SimpleCacheProcessor<Request,Response>{

//    Response returnCacheResult(Request request,Class<?> clazz);

    Response returnCacheResult(Request request, SimpleCache simpleCache, Method targetMethod);
    void putCacheResult(Request request, Response result, Object targetObject, Method targetMethod,SimpleCache simpleCache);


    void buildCacheProvide(Object cacheProvide);

    void buildEventPublisher(EventPublisher eventPublisher);

    String paramKeyByRequest(Request request);

}
