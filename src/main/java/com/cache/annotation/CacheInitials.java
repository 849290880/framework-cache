package com.cache.annotation;

import com.cache.CacheInitial;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheInitials {
    CacheInitial[] value();
}