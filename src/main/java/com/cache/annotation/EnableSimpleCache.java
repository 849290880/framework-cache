package com.cache.annotation;

import com.cache.CacheConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({CacheConfig.class})
public @interface EnableSimpleCache {
}