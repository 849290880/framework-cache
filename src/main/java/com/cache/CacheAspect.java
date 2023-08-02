package com.cache;

import cn.hutool.core.util.ReflectUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
public class CacheAspect {

    private static final Logger log = LoggerFactory.getLogger(CacheAspect.class);

    //TODO 后续可以抽象缓存这块，实现使用不同的缓存方法
//    @Autowired
    private final RedisTemplate<String,Object> cacheTemplate;

    public CacheAspect(RedisTemplate<String,Object> cacheTemplate){
        this.cacheTemplate = cacheTemplate;
    }

    @Pointcut("@annotation(com.cache.SimpleCache) || @annotation(com.cache.SimpleCacheInitial)")
    public void cachePointcut() {

    }

    @Around("cachePointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        //实例化缓存处理逻辑类,使用spring工具类合得到合并的注解
        SimpleCache simpleCache = AnnotatedElementUtils.findMergedAnnotation(method, SimpleCache.class);

        Class<?> clazz = simpleCache.clazz();
        CacheProcessor cacheProcessor = (CacheProcessor) clazz.getDeclaredConstructor().newInstance();
        //当这里的缓存需要使用不同的框架时候处理成使用不同的换工具
        cacheProcessor.buildCacheProvide(cacheTemplate);

        Object[] args = point.getArgs();
        Parameter[] parameters = method.getParameters();

        Object cacheParam = null;
        Object originalParam = null;
        for (int i = 0; i < parameters.length; i++) {
            //当使用该注解,并且参数只有一个时,这个参数当作cache的参数
            if(parameters.length == 1){
                cacheParam = args[0];
                break;
            }
            CacheParam annotation = parameters[i].getAnnotation(CacheParam.class);
            if (annotation != null) {
                //只处理第一个加注解的参数
                cacheParam = args[i];
                break;
            }
        }

        originalParam = cacheParam == null ? null : DeepCopy.copy(cacheParam);
        Object o = ReflectUtil.invoke(cacheProcessor, "returnCacheResult", originalParam,simpleCache,method);
        if(o != null){
            log.info("命中缓存");
            return o;
        }

        // 在这里，你可以使用 key 来查找缓存。如果缓存存在，返回缓存的结果。
        // 如果缓存不存在，执行方法并将结果存入缓存。

        // 执行方法
        Object result = point.proceed();

        // 存储结果到缓存
        if (simpleCache.putToCache()) {
            ReflectUtil.invoke(cacheProcessor, "putCacheResult", originalParam,
                    result,point.getTarget(),method,simpleCache);
        }

        return result;
    }


}
