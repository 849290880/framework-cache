package com.cache;


import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public interface CacheProcessor<Request,Response>{

//    Response returnCacheResult(Request request,Class<?> clazz);

    Response returnCacheResult(Request request,SimpleCache simpleCache,Method targetMethod);
    void putCacheResult(Request request, Object result, Object targetObject, Method targetMethod,SimpleCache simpleCache);

    void putToCache(Request request, Object result,String key,long timeout,TimeUnit timeUnit);

    void removeCache(String key);

    default void buildCacheProvide(Object cacheProvide){

    }

}
