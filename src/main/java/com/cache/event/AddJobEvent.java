package com.cache.event;

import com.cache.RefreshCache;
import org.springframework.context.ApplicationEvent;

public class AddJobEvent extends ApplicationEvent {

    private final RefreshCache refreshCache;

    private final String jobKey;

    public AddJobEvent(Object source, RefreshCache refreshCache, String jobKey) {
        super(source);
        this.refreshCache = refreshCache;
        this.jobKey = jobKey;
    }

    public RefreshCache getRefreshCache() {
        return refreshCache;
    }

    public String getJobKey() {
        return jobKey;
    }
}