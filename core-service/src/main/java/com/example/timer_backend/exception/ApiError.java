package com.example.timer_backend.exception;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class ApiError {
    private final HttpStatus status;
    private final LocalDateTime timestamp;
    private final String message;
    private final List<String> errors;

    public ApiError(
            final HttpStatus status,
            final String message,
            final String error
    ) {
        super();
        this.status = status;
        this.message = message;
        this.errors = Collections.singletonList(error);
        this.timestamp = LocalDateTime.now();
    }
}
