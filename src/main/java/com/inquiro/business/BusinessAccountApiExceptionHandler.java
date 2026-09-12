package com.inquiro.business;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = BusinessAccountController.class)
public class BusinessAccountApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(
            ResponseStatusException exception) {

        String reason = exception.getReason();

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.of(
                        "error",
                        reason == null
                                ? "Request could not be processed"
                                : reason
                ));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Map<String, String>> invalid(
            Exception exception) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "error",
                        "Invalid business onboarding request"
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> failed(
            Exception exception) {

        return ResponseEntity
                .internalServerError()
                .body(Map.of(
                        "error",
                        "Business onboarding request could not be completed"
                ));
    }
}