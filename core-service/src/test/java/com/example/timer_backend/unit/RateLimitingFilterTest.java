package com.example.timer_backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.timer_backend.filter.RateLimitingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitingFilterTest {
    private RateLimitingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void shouldAllowRequest_WhenUnderLimit() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("127.0.0.1");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @Disabled
    void shouldBlockRequest_WhenLimitExceeded() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");

        for (int i = 0; i < 10; i++) {
            filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), blockedResponse.getStatus());
        assertEquals("Too Many Requests - Rate limit exceeded", blockedResponse.getContentAsString());
        verify(filterChain, times(10)).doFilter(any(), any());
    }
}
