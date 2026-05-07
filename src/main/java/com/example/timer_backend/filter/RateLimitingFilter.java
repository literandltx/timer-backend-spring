package com.example.timer_backend.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true")
public class RateLimitingFilter implements Filter {
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = httpRequest.getRemoteAddr();
        String requestUri = httpRequest.getRequestURI();

        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> {
            log.info("Creating new rate-limit bucket for IP: {}", clientIp);
            return createNewBucket();
        });

        if (bucket.tryConsume(1)) {
            log.trace("Request allowed for IP: {} on URI: {}", clientIp, requestUri);
            chain.doFilter(request, response);
        } else {
            long availableTokens = bucket.getAvailableTokens();
            log.warn("RATE LIMIT EXCEEDED - IP: {} | URI: {} | Available Tokens: {}",
                    clientIp, requestUri, availableTokens);

            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("text/plain");
            httpResponse.getWriter().write("Too Many Requests - Rate limit exceeded");
        }
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(100)
                .refillIntervally(1, Duration.ofSeconds(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
