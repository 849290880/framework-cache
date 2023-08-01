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

    /**
     * 如果定义了切面，使用该属性调用原来的没有切面的方法
     * @return
     */
    boolean proxy() default true;

}
