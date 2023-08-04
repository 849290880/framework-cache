package com.cache.event;

import org.springframework.context.ApplicationEvent;

public class RefreshJobEvent extends ApplicationEvent {

    private final String jobKey;

    public RefreshJobEvent(Object source,String jobKey) {
        super(source);
        this.jobKey = jobKey;
    }

    public String getJobKey() {
        return jobKey;
    }
}