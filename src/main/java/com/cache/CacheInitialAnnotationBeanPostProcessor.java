package com.cache;

import com.cache.annotation.CacheInitials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

public class CacheInitialAnnotationBeanPostProcessor implements BeanPostProcessor, SmartInitializingSingleton,
        BeanFactoryAware, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(CacheInitialAnnotationBeanPostProcessor.class);
    private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));

    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    public static final String DEFAULT_CACHE_JOB_BEAN_NAME = "cacheScheduler";

    public static final String CACHE_TEMPLATE = "cacheTemplate";

    private RedisTemplate<String, Object> redisTemplate;

    private BeanFactory beanFactory;

    private ApplicationContext applicationContext;

    private List<CacheInitialProcessor> cacheInitialProcessorList = new CopyOnWriteArrayList<>();


    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (logger.isTraceEnabled()) {
            logger.trace("进入方法,bean的名称为:{}",beanName);
        }
        if (bean instanceof AopInfrastructureBean || bean instanceof TaskScheduler ||
                bean instanceof ScheduledExecutorService) {
            // Ignore AOP infrastructure such as scoped proxies.
            if (logger.isTraceEnabled()) {
                logger.trace("aopBean,aopBean的名称为:{}",beanName);
            }
            return bean;
        }

        //原始未被代理的类
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);

        if(!this.nonAnnotatedClasses.contains(targetClass) &&
                AnnotationUtils.isCandidateClass(targetClass, Collections.singletonList(CacheInitial.class))){

            if(logger.isTraceEnabled()){
                logger.trace("命中,类名为：{}",targetClass);
            }

            Map<Method, Set<CacheInitial>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                    (MethodIntrospector.MetadataLookup<Set<CacheInitial>>) method -> {
                        Set<CacheInitial> cacheInitialMethods = AnnotatedElementUtils.getMergedRepeatableAnnotations(
                                method, CacheInitial.class, CacheInitials.class);
                        return (!cacheInitialMethods.isEmpty() ? cacheInitialMethods : null);
                    });

            if(annotatedMethods.isEmpty()){
                this.nonAnnotatedClasses.add(targetClass);
                if (logger.isTraceEnabled()) {
                    logger.trace("No @CacheInitial annotations found on bean class: " + targetClass);
                }
            }else {
                //处理带有注解的bean
                annotatedMethods.forEach((method, cacheInitialMethods) ->
                        cacheInitialMethods.forEach(cacheInitial -> processCacheInitial(cacheInitial, method, bean)));
                if (logger.isTraceEnabled()) {
                    logger.trace(annotatedMethods.size() + " @CacheInitial methods processed on bean '" + beanName +
                            "': " + annotatedMethods);
                }
            }
        }
        return bean;
    }

    private void processCacheInitial(CacheInitial cacheInitial, Method method, Object bean) {
        logger.info("bean名称:{},方法名称:{}",bean,method.getDeclaringClass());
        Class<? extends CacheInitialProcessor> clazz = cacheInitial.clazz();
        CacheInitialProcessor cacheInitialProcessorAbstract = null;
        try {
            cacheInitialProcessorAbstract = clazz.newInstance();
            //如果注解上确定这个bean是否走切面增强等逻辑
            Object target = bean;
            if (cacheInitial.proxy()) {
                cacheInitialProcessorAbstract.init(cacheInitial,method,target);
            }else {
                try {
                    if (AopUtils.isAopProxy(target) && target instanceof Advised) {
                        target = ((Advised) target).getTargetSource().getTarget();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                cacheInitialProcessorAbstract.init(cacheInitial,method,target);
            }

        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if(!StringUtils.isEmpty(cacheInitial.cron())){
            cacheInitialProcessorList.add(cacheInitialProcessorAbstract);
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.nonAnnotatedClasses.clear();
        finishRegisterBean();
        initJob();
    }

    private void initJob() {
        for (CacheInitialProcessor initialProcessor : cacheInitialProcessorList) {
            initialProcessor.initCacheTool(redisTemplate);
            initialProcessor.initThreadPool(threadPoolTaskScheduler);
            initialProcessor.refresh();
        }
    }

    private void finishRegisterBean() {
        this.threadPoolTaskScheduler = resolveBean(this.beanFactory, ThreadPoolTaskScheduler.class,DEFAULT_CACHE_JOB_BEAN_NAME);
        this.redisTemplate = resolveBean(this.beanFactory,RedisTemplate.class,CACHE_TEMPLATE);
    }

    private <T> T resolveBean(BeanFactory beanFactory, Class<T> threadPoolTaskSchedulerType,String name) {
        return beanFactory.getBean(name, threadPoolTaskSchedulerType);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        if (this.beanFactory == null) {
            this.beanFactory = applicationContext;
        }
    }
}


