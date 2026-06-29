package com.literandltx.timer.service;

public interface RateLimiter {

    /**
     * Attempts to consume a single token for the given client identifier.
     *
     * @param clientId The unique identifier for the client (e.g., an IP address, API key, or user ID).
     * @return {@code true} if the token was successfully consumed (request allowed), 
     * {@code false} if the rate limit has been exceeded (request denied).
     */
    boolean tryConsume(String clientId);
    
}
