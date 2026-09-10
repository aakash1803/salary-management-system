package com.salarymanagement.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Centralized exception handling for the REST API.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so that exceptions raised
 * internally by Spring MVC itself (malformed requests, unsupported methods,
 * etc.) continue to receive Spring's own safe, structured handling, in
 * addition to the explicit handlers below for this application's own
 * exceptions. {@link #handleMethodArgumentNotValid} and
 * {@link #handleHttpMessageNotReadable} are overridden so that Bean Validation
 * failures and unreadable/malformed request bodies also produce this
 * application's {@link ErrorResponse} shape (rather than Spring's default
 * {@code ProblemDetail} body), keeping every 400/404/409/401/500 response in
 * one consistent format. No handler here ever includes a stack trace or
 * internal Spring/persistence detail in the response body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    /**
     * Handles {@code @Valid @RequestBody} Bean Validation failures (e.g. {@code @NotBlank},
     * {@code @Email}, {@code @Size} on {@link com.salarymanagement.employee.EmployeeRequest}),
     * returning the same {@link ErrorResponse} shape as every other error in this API instead of
     * Spring's default {@code ProblemDetail} body.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Validation failed.";
        }
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Handles malformed/unreadable request bodies (e.g. invalid JSON), returning the same
     * {@link ErrorResponse} shape rather than exposing the underlying parser's error detail.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request.", request);
    }

    private static String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<Object> build(HttpStatus status, String message, WebRequest request) {
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), message, pathOf(request));
        return ResponseEntity.status(status).body(body);
    }

    private String pathOf(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }
}
