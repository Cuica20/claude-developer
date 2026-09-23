package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;

import java.util.Optional;

public interface LoanValidator {
    Optional<FieldError> validate(LoanRequest request);
}
