package com.cache.annotation;

import com.cache.SimpleCacheInitialProcessor;

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

    String cron() default "0/10 * * * * *";

    /**
     * 将结果缓存的时间
     */
    long cacheTime() default 5L;

    /**
     * 缓存时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    /**
     * 初始化前删除以前的key
     * @return
     */
    boolean deletePreviousKey() default false;

    /**
     * 判断多少时间命中缓存就终止定时任务,单位为秒
     * @return
     */
    long ttlTime() default 3600L;

    boolean addToJob() default false;
}
