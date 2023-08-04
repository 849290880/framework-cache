package com.cache;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RefreshCache {

    private final static Logger log = LoggerFactory.getLogger(RefreshCache.class);

    private Method method;

    private Object targetObject;

    private Object paramObject;

    private String refreshKey;

    private CacheProcessor cacheProcessor;

    private volatile boolean freshCache;

    private String cron;

    private Integer fixTime;

    private long lastRefreshTime;

    private AtomicInteger cacheCount = new AtomicInteger(0);

    private long lastHitTime;

    private long cacheTime;

    private TimeUnit timeUnit;

    private ScheduledFuture scheduledFuture;

    private long ttl;

    public RefreshCache(){

    }


    //TODO 这个构造方法过于复杂，考虑使用建造者模式修改
    public RefreshCache(Method method, Object targetObject,
                        Object paramObject, String refreshKey,
                        CacheProcessor<?, ?> cacheProcessor, String cron,
                        Integer fixTime, long currentTime,
                        long cacheTime, TimeUnit timeUnit, long ttl) {
        this.method = method;
        this.targetObject = targetObject;
        this.paramObject = paramObject;
        this.refreshKey = refreshKey;
        this.cacheProcessor = cacheProcessor;
        this.cron = cron;
        this.fixTime = fixTime;
        this.lastRefreshTime = currentTime;
        this.lastHitTime = new Date().getTime();
        this.cacheTime = cacheTime;
        this.timeUnit = timeUnit;
        this.ttl = ttl;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setScheduledFuture(ScheduledFuture scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    public String getRefreshKey() {
        return refreshKey;
    }

    public Integer getFixTime() {
        return fixTime;
    }

    public String getCron() {
        return cron;
    }

    public boolean getFreshCache(){
        return this.freshCache;
    }

    public long getLastHitTime() {
        return lastHitTime;
    }

    public void saveLastHitTime(){
        lastHitTime = System.currentTimeMillis();
    }

    public AtomicInteger getCacheCount() {
        return cacheCount;
    }

    public void setCacheCount(AtomicInteger cacheCount) {
        this.cacheCount = cacheCount;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getTargetObject() {
        return targetObject;
    }

    public void setTargetObject(Object targetObject) {
        this.targetObject = targetObject;
    }

    public Object getParamObject() {
        return paramObject;
    }

    public void setParamObject(Object paramObject) {
        this.paramObject = paramObject;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RefreshCache that = (RefreshCache) o;
        return Objects.equals(refreshKey, that.refreshKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, targetObject, paramObject, refreshKey);
    }

    /**
     * 判断是否到达刷新时间
     */
    public boolean checkTimeToRefresh(){
        ZonedDateTime dateTimeToCheck = ZonedDateTime.now();
        if (isFix()) {
            DateTime dateTime = DateUtil.offsetSecond(new Date(lastRefreshTime), fixTime);
            Date date = new Date();
            String datePattern = "yyyy-MM-dd HH:mm:ss";
            int compare = DateUtil.compare(dateTime, date, datePattern);
            boolean timeToRefresh = compare == 0;
            log.info("执行的时间为:{},当前时间为:{},是否执行:{}",DateUtil.format(dateTime,datePattern),
                    DateUtil.format(date,datePattern),timeToRefresh);
            lastRefreshTime = dateTime.getTime();
            return timeToRefresh;
        }
        return isTimeMatchingCron(dateTimeToCheck,cron);
    }

    public boolean isCronTask(){
        return !StringUtils.isEmpty(cron);
    }

    public boolean isFix(){
        return StringUtils.isEmpty(cron) && !StringUtils.isEmpty(fixTime);
    }

    public boolean isTimeMatchingCron(ZonedDateTime dateTimeToCheck, String cronExpression) {
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
        CronParser parser = new CronParser(cronDefinition);
        Cron cron = parser.parse(cronExpression);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        ZonedDateTime nextExecution = executionTime.nextExecution(dateTimeToCheck.minusSeconds(1)).orElse(null);
        return nextExecution != null && nextExecution.equals(dateTimeToCheck);
    }

    public void cancel(){
        if(scheduledFuture!=null){
            scheduledFuture.cancel(false);
            //清除缓存上的数据
            cacheProcessor.removeCache(refreshKey);
        }
    }

    public void refresh() {
        if(freshCache){
            return;
        }
        synchronized (this){
            freshCache = true;
            Object result = null;
            try {
                if(paramObject == null){
                    result = this.method.invoke(targetObject);
                }else {
                    result = this.method.invoke(targetObject, paramObject);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            //将结果放入缓存
            cacheProcessor.putToCache(paramObject,result,refreshKey,cacheTime,timeUnit);
//            ReflectUtil.invoke(cacheProcessor, "putToCache", paramObject, result,
//                    refreshKey,cacheTime,timeUnit);
            freshCache = false;
            //更新上次刷新时间
            lastRefreshTime = System.currentTimeMillis();
        }
    }
}
