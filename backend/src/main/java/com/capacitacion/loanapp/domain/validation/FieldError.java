package com.capacitacion.loanapp.domain.validation;

public record FieldError(String field, String ruleId, String message) {
}
