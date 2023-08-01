package com.cache;


import java.lang.annotation.*;

/**
 * 这个注解，解决某一些接口再项目启动的时候，需要根据某些特定的参数 定时初始化到缓存的方法
 */
@Repeatable(CacheInitials.class)
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheInitial {

    Class<? extends CacheInitialProcessorAbstract> clazz() default CacheInitialProcessorAbstract.class;


    String cron() default "0/10 * * * * *";
}
