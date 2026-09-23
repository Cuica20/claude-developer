package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CreditScoreValidatorTest {

    private final CreditScoreValidator validator = new CreditScoreValidator();

    private LoanRequest requestWith(int creditScore, BigDecimal amount) {
        return new LoanRequest(
            "Test User", "test@test.com", LocalDate.now().minusYears(30),
            BigDecimal.valueOf(10000), creditScore, amount, 24);
    }

    @Test
    void should_returnError_when_scoreBelow600ForLowAmount() {
        Optional<FieldError> result = validator.validate(requestWith(550, BigDecimal.valueOf(20000)));

        assertTrue(result.isPresent());
        assertEquals("creditScore", result.get().field());
        assertEquals("RN-003", result.get().ruleId());
        assertTrue(result.get().message().contains("600"));
    }

    @Test
    void should_returnError_when_scoreBelow700ForHighAmount() {
        Optional<FieldError> result = validator.validate(requestWith(650, BigDecimal.valueOf(60000)));

        assertTrue(result.isPresent());
        assertTrue(result.get().message().contains("700"));
    }

    @Test
    void should_useLowThreshold_when_amountIsExactly50000() {
        Optional<FieldError> result = validator.validate(requestWith(600, BigDecimal.valueOf(50000)));

        assertTrue(result.isEmpty());
    }

    @Test
    void should_returnEmpty_when_scoreIsSufficient() {
        Optional<FieldError> result = validator.validate(requestWith(750, BigDecimal.valueOf(60000)));

        assertTrue(result.isEmpty());
    }
}
