package com.cache;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SimpleCache
@CacheInitial
public @interface SimpleCacheInitial {

    Class<? extends SimpleCacheInitialProcessor> clazz() default SimpleCacheInitialProcessor.class;


    boolean putToCache() default false;

    boolean proxy() default false;

    String prefixKey() default "";

    /**
     * 将结果缓存的时间
     */
    long cacheTime() default 5L;

    /**
     * 缓存时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;
}
