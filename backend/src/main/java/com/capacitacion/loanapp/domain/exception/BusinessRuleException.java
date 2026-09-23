package com.capacitacion.loanapp.domain.exception;

import lombok.Getter;

@Getter
public class BusinessRuleException extends RuntimeException {

    private final String ruleId;
    private final String field;

    public BusinessRuleException(String ruleId, String message) {
        super(message);
        this.ruleId = ruleId;
        this.field  = null;
    }

    public BusinessRuleException(String ruleId, String field, String message) {
        super(message);
        this.ruleId = ruleId;
        this.field  = field;
    }
}
