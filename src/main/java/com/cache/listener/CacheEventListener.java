package com.cache.listener;

import com.cache.CronConfig;
import com.cache.RefreshCache;
import com.cache.event.AddJobEvent;
import com.cache.event.RefreshJobEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.*;
import java.util.concurrent.*;

/**
 * 统计服务有比较多的首页报表，其中的的结果都需要复杂计算，
 * 本类将结果统计放入redis缓存中，注意缓存结果的key不能过大
 */
public class CacheEventListener {

    private static final Logger log = LoggerFactory.getLogger(CacheEventListener.class);

    public final Map<String,RefreshCache> refreshCacheMap = new ConcurrentHashMap<>();

    private final List<RefreshCache> refreshTask = new CopyOnWriteArrayList<>();

    @Autowired
    private CronConfig cronConfig;

    @Autowired
    @Qualifier(value = "cacheScheduler")
    private TaskScheduler cacheScheduler;

    @EventListener
    public void handleAddJobEvent(AddJobEvent event) {
        //防止添加多个任务
        if(refreshCacheMap.containsKey(event.getJobKey())){
            return;
        }
        refreshCacheMap.put(event.getJobKey(),event.getRefreshCache());
        log.debug("开启一个缓存任务,任务id:{}",event.getJobKey());
        //启动任务
        startRefreshCache();
    }

    @EventListener
    public void handleRefreshJobEvent(RefreshJobEvent event) {
        RefreshCache refreshCache = refreshCacheMap.get(event.getJobKey());
        if(refreshCache!=null){
            //记录缓存任务中最后缓存命中的时间
            refreshCache.getCacheCount().incrementAndGet();
            refreshCache.saveLastHitTime();
        }
    }


    public void startRefreshCache(){
        Collection<RefreshCache> refreshCacheList = refreshCacheMap.values();

        //控制线程数量 这里可以考虑使用动态线程池
        ThreadPoolTaskScheduler threadPoolTaskScheduler = null;
        if(cacheScheduler instanceof ThreadPoolTaskScheduler){
            threadPoolTaskScheduler = (ThreadPoolTaskScheduler)cacheScheduler;
            int size = refreshCacheList.size();
            if( size == 0){
                threadPoolTaskScheduler.setPoolSize(1);
            }
            if(size >=1){
                threadPoolTaskScheduler.setPoolSize(Math.min(size, 16));
            }
        }

        for (RefreshCache refreshCache : refreshCacheList) {
            if(refreshTask.contains(refreshCache)){
                continue;
            }
            if (refreshCache.isFix()) {
                refreshTask.add(refreshCache);
                ScheduledFuture<?> scheduledFuture = cacheScheduler.scheduleAtFixedRate(() -> {
                    try {
                        log.debug("开始刷新,任务名称为:{}", refreshCache.getRefreshKey());
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
                ScheduledFuture<?> scheduledFuture = cacheScheduler.schedule(() -> {
                    try {
                        log.debug("开始刷新,任务名称为:{}", refreshCache.getRefreshKey());
                        refreshCache.refresh();
                    } catch (Exception e) {
                        removeRefreshTask(refreshCache);
                    }
                }, new CronTrigger(refreshCache.getCron()));
                refreshCache.setScheduledFuture(scheduledFuture);
            }
        }

    }


//    @Scheduled(cron = "0 0/2 * * * *")
//    @Scheduled(cron = "#{cronConfig.cronExpression}")
    @Scheduled(cron = "0/10 * * * * *")
    public void refreshCacheToRedis(){
        log.debug("开始清理缓存任务");
        Collection<RefreshCache> refreshCacheList = refreshCacheMap.values();
        log.debug("缓存任务数量:{},正在执行的任务:{}",refreshCacheList.size(),refreshTask.size());
        //按规则清除任务
        removeRefreshCache();
        log.debug("清理缓存任务结束");
    }

    private void removeRefreshCache() {
        //清除超时间没有命中缓存的任务
        refreshCacheMap.keySet().removeIf(key -> {
            RefreshCache refreshCache = refreshCacheMap.get(key);
            long lastHitTime = refreshCache.getLastHitTime();
            long time = System.currentTimeMillis() - lastHitTime;
            // 12小时的毫秒数：12 * 60 * 60 * 1000 = 43200000
            // 30秒： 1000 * 30 = 30000
            if (time >= TimeUnit.SECONDS.toMillis(refreshCache.getTtl())) {
                log.debug("长时间缓存没有命中,清除该任务,任务名称为:{}",refreshCache.getRefreshKey());
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
                log.debug("缓存任务过多,清除该任务,任务名称为:{}",refreshCache.getRefreshKey());
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


    public void removeRefreshTask(RefreshCache refreshCache){
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
