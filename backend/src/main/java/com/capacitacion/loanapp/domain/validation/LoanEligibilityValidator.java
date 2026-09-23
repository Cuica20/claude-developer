package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Orquesta todas las reglas de negocio de una solicitud de préstamo (patrón Strategy). Corre las
 * reglas síncronas y la(s) asíncrona(s) en paralelo, y agrega todos los errores de campo en un
 * solo mapa — no hace fail-fast.
 */
@Component
public class LoanEligibilityValidator {

    private final List<LoanValidator> syncValidators;
    private final List<AsyncLoanValidator> asyncValidators;

    public LoanEligibilityValidator(List<LoanValidator> syncValidators,
                                     List<AsyncLoanValidator> asyncValidators) {
        this.syncValidators = syncValidators;
        this.asyncValidators = asyncValidators;
    }

    public Map<String, String> validate(LoanRequest request) {
        List<CompletableFuture<Optional<FieldError>>> asyncResults = asyncValidators.stream()
            .map(validator -> validator.validateAsync(request))
            .toList();

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (LoanValidator validator : syncValidators) {
            validator.validate(request)
                .ifPresent(error -> fieldErrors.put(error.field(), error.message()));
        }

        for (CompletableFuture<Optional<FieldError>> future : asyncResults) {
            future.join()
                .ifPresent(error -> fieldErrors.put(error.field(), error.message()));
        }

        return fieldErrors;
    }
}
