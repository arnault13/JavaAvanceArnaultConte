package com.university.reservation.exception;

import com.university.reservation.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .timestamp(OffsetDateTime.now())
                .path(request.getRequestURI())
                .details(ex.getDetails())
                .fieldErrors(Map.of())
                .build();
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        ApiErrorResponse response = ApiErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("La requete contient des donnees invalides")
                .timestamp(OffsetDateTime.now())
                .path(request.getRequestURI())
                .details(Map.of())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Parametre obligatoire manquant : " + ex.getParameterName())
                .timestamp(OffsetDateTime.now())
                .path(request.getRequestURI())
                .details(Map.of())
                .fieldErrors(Map.of(ex.getParameterName(), "est obligatoire"))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Valeur invalide pour le parametre : " + ex.getName())
                .timestamp(OffsetDateTime.now())
                .path(request.getRequestURI())
                .details(Map.of())
                .fieldErrors(Map.of(ex.getName(), "format invalide"))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
