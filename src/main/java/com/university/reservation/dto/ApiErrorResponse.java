package com.university.reservation.dto;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.Map;

@Builder
public record ApiErrorResponse(
    String code,
    String message,
    OffsetDateTime timestamp,
    String path,
    Map<String, Object> details,
    Map<String, String> fieldErrors
) {}
