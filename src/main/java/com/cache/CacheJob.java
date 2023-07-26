package com.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * 统计服务有比较多的首页报表，其中的的结果都需要复杂计算，
 * 本类将结果统计放入redis缓存中，注意缓存结果的key不能过大
 */
public class CacheJob {

    private static final Logger log = LoggerFactory.getLogger(CacheJob.class);

    public final static Map<String,RefreshCache> refreshCacheMap = new ConcurrentHashMap<>();

    private static final List<RefreshCache> refreshTask = new CopyOnWriteArrayList<>();

    @Autowired
    private CronConfig cronConfig;

    private static final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

    static {
        threadPoolTaskScheduler.setPoolSize(1);
        threadPoolTaskScheduler.setThreadNamePrefix("cacheTask");
        threadPoolTaskScheduler.initialize();
    }


    public void startRefreshCache(){
        Collection<RefreshCache> refreshCacheList = refreshCacheMap.values();
        if(refreshCacheList.size() == 0){
            threadPoolTaskScheduler.setPoolSize(1);
        }
        if(refreshCacheList.size() >=1){
            threadPoolTaskScheduler.setPoolSize(Math.min(refreshCacheList.size(), 30));
        }
        for (RefreshCache refreshCache : refreshCacheList) {
            if(refreshTask.contains(refreshCache)){
                continue;
            }
            if (refreshCache.isFix()) {
                refreshTask.add(refreshCache);
                ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
                    try {
                        log.info("开始刷新,任务名称为:{}", refreshCache.getRefreshKey());
                        refreshCache.refresh();
                    } catch (Exception e) {
                        log.error("刷新失败", e);
                        removeRefreshTask(refreshCache);
                    }

                }, TimeUnit.SECONDS.toMillis(refreshCache.getFixTime()));

                refreshCache.setScheduledFuture(scheduledFuture);
            }

            if(refreshCache.isCronTask()){
                refreshTask.add(refreshCache);
                ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.schedule(() -> {
                    try {
                        log.info("开始刷新,任务名称为:{}", refreshCache.getRefreshKey());
                        refreshCache.refresh();
                    } catch (Exception e) {
                        removeRefreshTask(refreshCache);
                    }
                }, new CronTrigger(refreshCache.getCron()));
                refreshCache.setScheduledFuture(scheduledFuture);
            }
        }

    }


    /**
     * 统计服务的定时任务为 59 0/1 * * *
     * 一分钟调用，将结果进行缓存
     */
//    @Scheduled(cron = "0 0/2 * * * *")
//    @Scheduled(cron = "#{cronConfig.cronExpression}")
    @Scheduled(cron = "0/10 * * * * *")
    public void refreshCacheToRedis(){
        log.info("开始刷新缓存");
        Collection<RefreshCache> refreshCacheList = refreshCacheMap.values();
        log.info("缓存任务数量:{},正在执行的任务:{}",refreshCacheList.size(),refreshTask.size());
        //启动任务
        startRefreshCache();
        //按规则清除任务
        removeRefreshCache();

        log.info("刷新缓存结束");
    }

    private static void removeRefreshCache() {


        //清除超时间没有命中缓存的任务
        refreshCacheMap.keySet().removeIf(key -> {
            RefreshCache refreshCache = refreshCacheMap.get(key);
            long lastHitTime = refreshCache.getLastHitTime();
            long time = System.currentTimeMillis() - lastHitTime;
            // 12小时的毫秒数：12 * 60 * 60 * 1000 = 43200000
            // 30秒： 1000 * 30 = 30000
            if (time >= 30000) {
                log.info("长时间缓存没有命中,清除该任务,任务名称为:{}",refreshCache.getRefreshKey());
                refreshCache.cancel();
                refreshTask.remove(refreshCache);
                return true;
            }
            return false;
        });


        PriorityQueue<RefreshCache> saveQueue = new PriorityQueue<>(Comparator.comparingInt(value ->
                (int) value.getLastHitTime()));
        for (RefreshCache refreshCache : refreshCacheMap.values()) {
            saveQueue.add(refreshCache);
            while (saveQueue.size() > 30) {
                RefreshCache poll = saveQueue.poll();
                //请求中断线程
                log.info("缓存任务过多,清除该任务,任务名称为:{}",refreshCache.getRefreshKey());
                refreshTask.remove(poll);
                refreshCache.cancel();
            }
        }
        //如果任务数量大于150个将清除最近没有命中缓存的任务
        refreshCacheMap.keySet().removeIf(key -> {
            RefreshCache refreshCache = refreshCacheMap.get(key);
            if (!saveQueue.contains(refreshCache)) {
                refreshTask.remove(refreshCache);
                //请求中断线程
                refreshCache.cancel();
                return true;
            }
            return false;
        });
    }


    public static void removeRefreshTask(RefreshCache refreshCache){
        refreshCacheMap.keySet().removeIf(key -> {
            RefreshCache cache = refreshCacheMap.get(key);
            if (Objects.equals(refreshCache, cache)) {
                //去掉正在运行的任务
                refreshTask.remove(refreshCache);
                //请求中断线程
                refreshCache.cancel();
                return true;
            }
            return false;
        });
    }


}
