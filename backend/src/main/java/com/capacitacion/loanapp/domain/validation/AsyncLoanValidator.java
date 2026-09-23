package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AsyncLoanValidator {
    CompletableFuture<Optional<FieldError>> validateAsync(LoanRequest request);
}
