package com.cache;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 用于做简单的缓存功能
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface SimpleCache {


    Class<? extends CacheProcessor> clazz() default CommonCacheProcessor.class;

    /**
     * 是否加入缓存任务
     */
    boolean addToJob() default false;

    /**
     * 将结果缓存的时间
     */
    long cacheTime() default 5L;

    /**
     * 缓存时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    String prefixKey() default "";

    String cron() default "";

    /**
     * 默认位5秒,单位为秒
     * @return
     */
    long fixTime() default 5L;


}
