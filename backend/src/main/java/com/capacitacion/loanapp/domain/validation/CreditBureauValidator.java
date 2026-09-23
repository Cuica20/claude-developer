package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;
import com.capacitacion.loanapp.domain.exception.CreditBureauUnavailableException;
import com.capacitacion.loanapp.domain.service.CreditBureauService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * RN-004: el solicitante no debe tener deuda vigente en mora, según el buró de crédito.
 */
@Component
public class CreditBureauValidator implements AsyncLoanValidator {

    private final CreditBureauService creditBureauService;

    public CreditBureauValidator(CreditBureauService creditBureauService) {
        this.creditBureauService = creditBureauService;
    }

    @Override
    public CompletableFuture<Optional<FieldError>> validateAsync(LoanRequest request) {
        return creditBureauService.hasActiveDebt(request.applicantEmail())
            .thenApply(hasActiveDebt -> hasActiveDebt
                ? Optional.of(new FieldError(
                    "applicantEmail",
                    "RN-004",
                    "RN-004: El solicitante registra deuda vigente en el buró de crédito"))
                : Optional.<FieldError>empty())
            .exceptionally(ex -> {
                throw new CreditBureauUnavailableException(
                    "Fallo al consultar el buró de crédito", ex);
            });
    }
}
