package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AgeValidatorTest {

    private final AgeValidator validator = new AgeValidator();

    private LoanRequest requestWithBirthDate(LocalDate birthDate) {
        return new LoanRequest(
            "Test User", "test@test.com", birthDate,
            BigDecimal.valueOf(3000), 700, BigDecimal.valueOf(10000), 24);
    }

    @Test
    void should_returnError_when_applicantIsUnder18() {
        LocalDate birthDate = LocalDate.now().minusYears(17);

        Optional<FieldError> result = validator.validate(requestWithBirthDate(birthDate));

        assertTrue(result.isPresent());
        assertEquals("birthDate", result.get().field());
        assertEquals("RN-001", result.get().ruleId());
    }

    @Test
    void should_returnEmpty_when_applicantIsExactly18Today() {
        LocalDate birthDate = LocalDate.now().minusYears(18);

        Optional<FieldError> result = validator.validate(requestWithBirthDate(birthDate));

        assertTrue(result.isEmpty());
    }

    @Test
    void should_returnEmpty_when_applicantIsOlderThan18() {
        LocalDate birthDate = LocalDate.now().minusYears(30);

        Optional<FieldError> result = validator.validate(requestWithBirthDate(birthDate));

        assertTrue(result.isEmpty());
    }
}
