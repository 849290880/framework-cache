package com.cache;

import com.cache.event.AddJobEvent;
import com.cache.event.RefreshJobEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;


public class EventPublisher {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishAddJobEvent(RefreshCache refreshCache, String jobKey) {
        AddJobEvent refreshJobEvent = new AddJobEvent(this, refreshCache,jobKey);
        applicationEventPublisher.publishEvent(refreshJobEvent);
    }


    public void publishRefreshJobEvent(String jobKey) {
        RefreshJobEvent refreshJobEvent = new RefreshJobEvent(this,jobKey);
        applicationEventPublisher.publishEvent(refreshJobEvent);
    }
}