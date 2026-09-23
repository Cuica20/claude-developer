package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class LoanEligibilityValidatorTest {

    private final LoanRequest sampleRequest = new LoanRequest(
        "Test User", "test@test.com", LocalDate.now().minusYears(30),
        BigDecimal.valueOf(10000), 700, BigDecimal.valueOf(20000), 24);

    private LoanValidator alwaysPasses() {
        return req -> Optional.empty();
    }

    private LoanValidator alwaysFails(String field, String ruleId) {
        return req -> Optional.of(new FieldError(field, ruleId, ruleId + ": falló"));
    }

    private AsyncLoanValidator asyncAlwaysPasses() {
        return req -> CompletableFuture.completedFuture(Optional.empty());
    }

    private AsyncLoanValidator asyncAlwaysFails(String field, String ruleId) {
        return req -> CompletableFuture.completedFuture(
            Optional.of(new FieldError(field, ruleId, ruleId + ": falló")));
    }

    @Test
    void should_returnEmptyMap_when_noValidatorFails() {
        LoanEligibilityValidator validator = new LoanEligibilityValidator(
            List.of(alwaysPasses()), List.of(asyncAlwaysPasses()));

        Map<String, String> result = validator.validate(sampleRequest);

        assertTrue(result.isEmpty());
    }

    @Test
    void should_returnOneError_when_oneValidatorFails() {
        LoanEligibilityValidator validator = new LoanEligibilityValidator(
            List.of(alwaysFails("creditScore", "RN-003")), List.of(asyncAlwaysPasses()));

        Map<String, String> result = validator.validate(sampleRequest);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("creditScore"));
    }

    @Test
    void should_aggregateAllErrors_when_allValidatorsFail() {
        LoanEligibilityValidator validator = new LoanEligibilityValidator(
            List.of(
                alwaysFails("birthDate", "RN-001"),
                alwaysFails("monthlyIncome", "RN-002"),
                alwaysFails("creditScore", "RN-003")),
            List.of(asyncAlwaysFails("applicantEmail", "RN-004")));

        Map<String, String> result = validator.validate(sampleRequest);

        assertEquals(4, result.size());
        assertTrue(result.containsKey("birthDate"));
        assertTrue(result.containsKey("monthlyIncome"));
        assertTrue(result.containsKey("creditScore"));
        assertTrue(result.containsKey("applicantEmail"));
    }

    @Test
    void should_propagateException_when_asyncValidatorFails() {
        AsyncLoanValidator failing = req -> {
            CompletableFuture<Optional<FieldError>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("buró caído"));
            return future;
        };
        LoanEligibilityValidator validator = new LoanEligibilityValidator(
            List.of(alwaysPasses()), List.of(failing));

        assertThrows(CompletionException.class, () -> validator.validate(sampleRequest));
    }
}
