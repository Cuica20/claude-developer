package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

/**
 * RN-001: el solicitante debe ser mayor de 18 años.
 */
@Component
public class AgeValidator implements LoanValidator {

    private static final int MIN_AGE = 18;

    @Override
    public Optional<FieldError> validate(LoanRequest request) {
        int age = Period.between(request.birthDate(), LocalDate.now()).getYears();
        if (age < MIN_AGE) {
            return Optional.of(new FieldError(
                "birthDate",
                "RN-001",
                "RN-001: El solicitante debe ser mayor de 18 años"));
        }
        return Optional.empty();
    }
}
