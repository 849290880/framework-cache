package com.cache;

import java.util.concurrent.TimeUnit;

public interface CacheProcessor<Request,Response>  {

    void putToCache(Request request, Response result, String key, long timeout, TimeUnit timeUnit);

    void removeCache(String key);

}

