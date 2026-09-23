package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * RN-002: el ingreso mensual debe ser al menos 3 veces la cuota mensual.
 */
@Component
public class IncomeRatioValidator implements LoanValidator {

    private static final BigDecimal MIN_INCOME_MULTIPLIER = BigDecimal.valueOf(3);

    @Override
    public Optional<FieldError> validate(LoanRequest request) {
        BigDecimal cuota = request.amount()
            .divide(BigDecimal.valueOf(request.termMonths()), 2, RoundingMode.HALF_UP);
        BigDecimal minIncome = cuota.multiply(MIN_INCOME_MULTIPLIER);

        if (request.monthlyIncome().compareTo(minIncome) < 0) {
            return Optional.of(new FieldError(
                "monthlyIncome",
                "RN-002",
                "RN-002: Ingreso insuficiente. Minimo requerido: " + minIncome));
        }
        return Optional.empty();
    }
}
