package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IncomeRatioValidatorTest {

    private final IncomeRatioValidator validator = new IncomeRatioValidator();

    private LoanRequest requestWith(BigDecimal monthlyIncome, BigDecimal amount, int termMonths) {
        return new LoanRequest(
            "Test User", "test@test.com", LocalDate.now().minusYears(30),
            monthlyIncome, 700, amount, termMonths);
    }

    @Test
    void should_returnError_when_incomeIsBelowThreeTimesInstallment() {
        // cuota = 10000/24 = 416.67, minIncome = 1250.01
        Optional<FieldError> result = validator.validate(
            requestWith(BigDecimal.valueOf(1000), BigDecimal.valueOf(10000), 24));

        assertTrue(result.isPresent());
        assertEquals("monthlyIncome", result.get().field());
        assertEquals("RN-002", result.get().ruleId());
    }

    @Test
    void should_returnEmpty_when_incomeIsExactlyThreeTimesInstallment() {
        // amount=12000, termMonths=12 -> cuota=1000.00, minIncome=3000.00
        Optional<FieldError> result = validator.validate(
            requestWith(BigDecimal.valueOf(3000), BigDecimal.valueOf(12000), 12));

        assertTrue(result.isEmpty());
    }

    @Test
    void should_returnEmpty_when_incomeIsSufficient() {
        Optional<FieldError> result = validator.validate(
            requestWith(BigDecimal.valueOf(5000), BigDecimal.valueOf(10000), 24));

        assertTrue(result.isEmpty());
    }

    @Test
    void should_handleInstallmentWithRepeatingDecimals() {
        // amount=10000, termMonths=3 -> cuota=3333.33, minIncome=9999.99
        Optional<FieldError> result = validator.validate(
            requestWith(BigDecimal.valueOf(9999.98), BigDecimal.valueOf(10000), 3));

        assertTrue(result.isPresent());
        assertEquals("monthlyIncome", result.get().field());
    }
}
