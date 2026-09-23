package com.capacitacion.loanapp.domain.validation;

import com.capacitacion.loanapp.api.dto.LoanRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * RN-003: el score crediticio mínimo depende del monto solicitado.
 */
@Component
public class CreditScoreValidator implements LoanValidator {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = BigDecimal.valueOf(50_000);
    private static final int HIGH_AMOUNT_MIN_SCORE = 700;
    private static final int LOW_AMOUNT_MIN_SCORE = 600;

    @Override
    public Optional<FieldError> validate(LoanRequest request) {
        int minScore = request.amount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0
            ? HIGH_AMOUNT_MIN_SCORE
            : LOW_AMOUNT_MIN_SCORE;

        if (request.creditScore() < minScore) {
            return Optional.of(new FieldError(
                "creditScore",
                "RN-003",
                "RN-003: Score insuficiente. Minimo: " + minScore));
        }
        return Optional.empty();
    }
}
