package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;
import com.capacitacion.loanapp.domain.exception.CreditBureauUnavailableException;
import com.capacitacion.loanapp.domain.service.CreditBureauService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditBureauValidatorTest {

    @Mock
    private CreditBureauService creditBureauService;

    private LoanRequest sampleRequest() {
        return new LoanRequest(
            "Test User", "juan@moroso.test", LocalDate.now().minusYears(30),
            BigDecimal.valueOf(10000), 700, BigDecimal.valueOf(20000), 24);
    }

    @Test
    void should_returnFieldError_when_applicantHasActiveDebt() {
        when(creditBureauService.hasActiveDebt("juan@moroso.test"))
            .thenReturn(CompletableFuture.completedFuture(true));
        CreditBureauValidator validator = new CreditBureauValidator(creditBureauService);

        Optional<FieldError> result = validator.validateAsync(sampleRequest()).join();

        assertTrue(result.isPresent());
        assertEquals("applicantEmail", result.get().field());
        assertEquals("RN-004", result.get().ruleId());
    }

    @Test
    void should_returnEmpty_when_applicantHasNoActiveDebt() {
        when(creditBureauService.hasActiveDebt("juan@moroso.test"))
            .thenReturn(CompletableFuture.completedFuture(false));
        CreditBureauValidator validator = new CreditBureauValidator(creditBureauService);

        Optional<FieldError> result = validator.validateAsync(sampleRequest()).join();

        assertTrue(result.isEmpty());
    }

    @Test
    void should_propagateCreditBureauUnavailableException_when_serviceFails() {
        CompletableFuture<Boolean> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("timeout"));
        when(creditBureauService.hasActiveDebt("juan@moroso.test")).thenReturn(failed);
        CreditBureauValidator validator = new CreditBureauValidator(creditBureauService);

        CompletionException ex = assertThrows(CompletionException.class,
            () -> validator.validateAsync(sampleRequest()).join());
        assertInstanceOf(CreditBureauUnavailableException.class, ex.getCause());
    }
}
