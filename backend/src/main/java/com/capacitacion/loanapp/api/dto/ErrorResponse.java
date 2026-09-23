package com.capacitacion.loanapp.api.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
    int status,
    String error,
    String message,
    String ruleId,
    Map<String, String> fieldErrors,
    LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, null, null, LocalDateTime.now());
    }
    public static ErrorResponse ofRule(int status, String ruleId, String message) {
        return new ErrorResponse(status, "BusinessRuleViolation", message, ruleId, null, LocalDateTime.now());
    }
    public static ErrorResponse ofFields(int status, Map<String, String> fieldErrors) {
        return new ErrorResponse(status, "ValidationError", "Errores de validación", null, fieldErrors, LocalDateTime.now());
    }
}
