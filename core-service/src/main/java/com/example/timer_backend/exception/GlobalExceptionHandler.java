package com.example.timer_backend.exception;

import com.example.timer_backend.exception.custom.FileProcessingException;
import com.example.timer_backend.exception.custom.FileStorageException;
import com.example.timer_backend.exception.custom.InvalidFileFormatException;
import com.example.timer_backend.exception.custom.UnsupportedFileExtensionException;
import com.example.timer_backend.exception.custom.UserAlreadyExistsException;
import com.example.timer_backend.provider.FileType;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Default Global Exception Handler
     */
    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAll(
            final Exception exception,
            final WebRequest request
    ) {
        final ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getLocalizedMessage(),
                "Unexpected error occurred."
        );

        log.info("Exception was handled: {}", exception.getMessage());

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFound(
            final EntityNotFoundException exception,
            final WebRequest request
    ) {
        final ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND,
                exception.getLocalizedMessage(),
                "Resource not found."
        );

        log.info("Entity not found: {}", exception.getMessage());

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Object> handleUsernameNotFound(
            final UsernameNotFoundException exception,
            final WebRequest request
    ) {
        final ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND,
                exception.getLocalizedMessage(),
                "User not found."
        );

        log.info("Username not found: {}", exception.getMessage());

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(
            final AccessDeniedException exception,
            final WebRequest request
    ) {
        final ApiError apiError = new ApiError(
                HttpStatus.FORBIDDEN,
                exception.getLocalizedMessage(),
                "Access is denied."
        );

        log.info("Access denied: {}", exception.getMessage());

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleBadRequest(
            final HttpMessageNotReadableException exception,
            final WebRequest request
    ) {
        final ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                exception.getLocalizedMessage(),
                "Invalid request format."
        );

        log.info("Deserialization error: {}", exception.getMessage());

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Object> handleUserAlreadyExists(
            final UserAlreadyExistsException exception,
            final WebRequest request
    ) {
        final ApiError apiError = new ApiError(
                HttpStatus.CONFLICT,
                exception.getLocalizedMessage(),
                "User already exists."
        );

        log.info("Registration failed: {}", exception.getMessage());

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentials(
            final BadCredentialsException exception,
            final WebRequest request
    ) {
        final ApiError apiError = new ApiError(
                HttpStatus.UNAUTHORIZED,

                exception.getLocalizedMessage(),
                "Authentication failed. Incorrect username or password."
        );

        log.info("Bad credentials: {}", exception.getMessage());

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    /**
     * Handles @Valid validation errors (e.g. @Email, @Size, @FieldMatch)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationErrors(
            final MethodArgumentNotValidException exception,
            final WebRequest request
    ) {
        List<String> errors = new ArrayList<>();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }

        for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }

        String errorMessage = "Validation failed for: " + String.join(", ", errors);

        final ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                errorMessage,
                "Input validation failed"
        );

        log.info("Validation failed: {}", errors);

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler(InvalidFileFormatException.class)
    public ResponseEntity<Object> handleInvalidFileFormat(
            final InvalidFileFormatException exception,
            final WebRequest request
    ) {
        final ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST,
                exception.getLocalizedMessage(),
                "The file content is invalid or corrupted."
        );

        log.warn("Invalid file format: {}", exception.getMessage());

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler(UnsupportedFileExtensionException.class)
    public ResponseEntity<Object> handleUnsupportedFileExtension(
            final UnsupportedFileExtensionException exception,
            final WebRequest request
    ) {
        final ApiError apiError = new ApiError(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                exception.getLocalizedMessage(),
                "The provided file extension is not supported."
        );

        log.info("Unsupported file extension: {}", exception.getMessage());

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Object> handleFileStorage(
            final FileStorageException exception,
            final WebRequest request
    ) {
        final ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getLocalizedMessage(),
                "Could not process or store the file."
        );

        log.error("File storage error: {}", exception.getMessage());

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<Object> handleFileProcessing(
            final FileProcessingException exception,
            final WebRequest request
    ) {
        final ApiError apiError = new ApiError(
                HttpStatus.UNPROCESSABLE_CONTENT,
                exception.getLocalizedMessage(),
                "Error occurred during file processing."
        );

        log.error("File processing error: {}", exception.getMessage());

        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if (ex.getRequiredType() != null && ex.getRequiredType().equals(FileType.class)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid file format",
                    "message", "Supported formats are: CSV, JSON"
            ));
        }
        return ResponseEntity.badRequest().build();
    }
}
