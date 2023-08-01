package com.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
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

    private List<CacheInitialProcessorAbstract> cacheInitialProcessorList = new CopyOnWriteArrayList<>();


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
                        cacheInitialMethods.forEach(scheduled -> processCacheInitial(scheduled, method, bean)));
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
        Class<? extends CacheInitialProcessorAbstract> clazz = cacheInitial.clazz();
        CacheInitialProcessorAbstract cacheInitialProcessorAbstract = null;
        try {
            cacheInitialProcessorAbstract = clazz.newInstance();
            cacheInitialProcessorAbstract.init(cacheInitial,method,bean);
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
        for (CacheInitialProcessorAbstract initialProcessor : cacheInitialProcessorList) {
            initialProcessor.initCacheTool(redisTemplate);
            threadPoolTaskScheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    Object param = initialProcessor.initialRequestParam();
                    Object result = null;
                    try {
                        if(param == null){
                            result = initialProcessor.getMethod().invoke(initialProcessor.getBean());
                        }else {
                            result = initialProcessor.getMethod().invoke(initialProcessor.getBean(), param);
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                    initialProcessor.saveToCache(result);
                }
            },new CronTrigger(initialProcessor.getCacheInitial().cron()));
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

