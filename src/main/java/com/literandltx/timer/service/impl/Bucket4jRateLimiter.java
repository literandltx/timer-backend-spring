package com.literandltx.timer.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.literandltx.timer.config.env.RateLimitProperties;
import com.literandltx.timer.service.RateLimiter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

@Service
public class Bucket4jRateLimiter implements RateLimiter {

    private final Cache<String, Bucket> cache;
    private final RateLimitProperties properties;

    public Bucket4jRateLimiter(RateLimitProperties properties) {
        this.properties = properties;
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.cacheMaxSize())
                .expireAfterAccess(properties.cacheExpire())
                .build();
    }

    public boolean tryConsume(String ipAddress) {
        return cache.get(ipAddress, this::createNewBucket)
                .tryConsume(1);
    }

    private Bucket createNewBucket(String ipAddress) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(properties.bucketCapacity())
                .refillIntervally(properties.bucketCapacity(), properties.refillInterval())
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
