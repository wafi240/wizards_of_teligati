package com.teligati.backend.exception;

import com.teligati.backend.common.ApiError;
import com.teligati.backend.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(error.getField() + ": " + error.getDefaultMessage());
        }

        ApiError error = ApiError.builder()
                .code("VALIDATION_FAILED")
                .message("Input validation failed")
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error, "Invalid request payload"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = new ArrayList<>();
        ex.getConstraintViolations().forEach(cv -> details.add(cv.getPropertyPath() + ": " + cv.getMessage()));

        ApiError error = ApiError.builder()
                .code("CONSTRAINT_VIOLATION")
                .message("Request parameter constraint violated")
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error, "Constraint violation"));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        ApiError error = ApiError.builder()
                .code("DATA_INTEGRITY_VIOLATION")
                .message("A database integrity constraint was violated: a record with the same unique identifier already exists.")
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(error, "Conflict: duplicate unique record"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleMalformedJson(HttpMessageNotReadableException ex) {
        ApiError error = ApiError.builder()
                .code("MALFORMED_JSON")
                .message("Malformed JSON request body")
                .details(List.of("The request body could not be parsed. Verify format and types."))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error, "Malformed request"));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleBadRequest(BadRequestException ex) {
        ApiError error = ApiError.builder()
                .code("BAD_REQUEST")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error, ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleNotFound(ResourceNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("RESOURCE_NOT_FOUND")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(error, ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleNoResourceFound(NoResourceFoundException ex) {
        ApiError error = ApiError.builder()
                .code("ENDPOINT_NOT_FOUND")
                .message("The requested endpoint was not found: " + ex.getResourcePath())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(error, "Resource not found"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ApiError error = ApiError.builder()
                .code("METHOD_NOT_ALLOWED")
                .message("HTTP method " + ex.getMethod() + " is not supported for this endpoint")
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(error, "Method not allowed"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleAccessDenied(AccessDeniedException ex) {
        ApiError error = ApiError.builder()
                .code("FORBIDDEN")
                .message("You do not have permission to access this resource")
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(error, "Access denied"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleBadCredentials(BadCredentialsException ex) {
        ApiError error = ApiError.builder()
                .code("UNAUTHORIZED")
                .message(ex.getMessage() != null ? ex.getMessage() : "Invalid credentials")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(error, "Authentication failed"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleAuthenticationException(AuthenticationException ex) {
        ApiError error = ApiError.builder()
                .code("UNAUTHORIZED")
                .message("Full authentication is required to access this resource")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(error, "Unauthorized"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ApiError>> handleGenericException(Exception ex) {
        log.error("Unhandled exception intercepted by GlobalExceptionHandler", ex);

        ApiError error = ApiError.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("An unexpected internal error occurred. Please contact the platform administrator.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(error, "Internal server error"));
    }
}
