package com.literandltx.timer.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.literandltx.timer.config.env.AppProperties;
import com.literandltx.timer.service.RateLimiter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

@Service
public class Bucket4jRateLimiter implements RateLimiter {

    private final Cache<String, Bucket> cache;
    private final AppProperties properties;

    public Bucket4jRateLimiter(AppProperties properties) {
        this.properties = properties;
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.rateLimit().cacheMaxSize())
                .expireAfterAccess(properties.rateLimit().cacheExpire())
                .build();
    }

    public boolean tryConsume(String ipAddress) {
        return cache.get(ipAddress, this::createNewBucket)
                .tryConsume(1);
    }

    private Bucket createNewBucket(String ipAddress) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(properties.rateLimit().bucketCapacity())
                .refillIntervally(properties.rateLimit().bucketCapacity(), properties.rateLimit().refillInterval())
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
