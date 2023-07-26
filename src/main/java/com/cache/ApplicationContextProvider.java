package com.cache;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    private static boolean isContextRefreshed = false;

    public static boolean isContextRefreshed() {
        return isContextRefreshed;
    }

    public static void setContextRefreshed(boolean contextRefreshed) {
        isContextRefreshed = contextRefreshed;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    public static <T> T getBean(String beanName,Class<T> beanClass){
        return applicationContext.getBean(beanName,beanClass);
    }


    public static void registerApplicationContext(ApplicationContext applicationContext) {
        if (applicationContext instanceof AbstractApplicationContext) {
            AbstractApplicationContext abstractContext = (AbstractApplicationContext) applicationContext;
            abstractContext.addApplicationListener((ApplicationListener<ContextRefreshedEvent>) event -> {
                if (event.getSource() == applicationContext && event.getApplicationContext() == applicationContext) {
                    setContextRefreshed(true);
                }
            });
            if (abstractContext instanceof GenericApplicationContext) {
                ((GenericApplicationContext) abstractContext).registerShutdownHook();
            }
        }
    }
}