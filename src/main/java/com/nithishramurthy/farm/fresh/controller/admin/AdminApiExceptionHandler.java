package com.nithishramurthy.farm.fresh.controller.admin;

import com.nithishramurthy.farm.fresh.exception.NotAllowedException;
import com.nithishramurthy.farm.fresh.exception.ResourceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions from the admin and owner APIs into {"message": ...} bodies
 * with the right status, instead of a try/catch in every endpoint:
 * not found -> 404, bad input -> 400, role doesn't allow it -> 403,
 * not allowed right now -> 409.
 */
@RestControllerAdvice(basePackageClasses = AdminApiExceptionHandler.class)
public class AdminApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminApiExceptionHandler.class);

    /** The product, order or user id doesn't exist. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * Bad input: our own checks throw IllegalArgumentException, and Spring throws the
     * other two for unreadable JSON or a parameter of the wrong type (e.g. "abc" for an id).
     * Spring's own messages are replaced, because they describe Java types, not the problem.
     */
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> badRequest(Exception e) {
        String message = e instanceof IllegalArgumentException ? e.getMessage() : "Request body or parameter is not valid.";
        return body(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * A @Valid request body broke a rule (blank name, negative price, ...). The reply lists
     * each bad field with its message, so the dashboard can show it next to the input.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err -> fields.putIfAbsent(err.getField(), err.getDefaultMessage()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Please fix the highlighted fields.");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    /** Logged in with the right role for the URL, but not allowed to do this particular thing (e.g. an admin making someone an owner). */
    @ExceptionHandler(NotAllowedException.class)
    public ResponseEntity<Map<String, Object>> forbidden(NotAllowedException e) {
        return body(HttpStatus.FORBIDDEN, e.getMessage());
    }

    /** The request is fine but the current state forbids it, e.g. moving a SHIPPED order back to PENDING. 409 Conflict. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> conflict(IllegalStateException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Anything else is a bug. Log the full stack trace for us, but never send it to the browser. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        log.error("Admin API request failed", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
