package com.capacitacion.loanapp.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests para LoanCalculatorService.
 * M7 — Debugging con Claude Code.
 *
 * Ejecutar: mvn test -Dtest=LoanCalculatorServiceTest
 * Resultado esperado con el código buggy: FALLA en testHighValueLoan
 *
 * Flujo del ejercicio:
 *   1. Ejecutar los tests → ver el fallo
 *   2. Copiar el stacktrace y el código fuente
 *   3. Pedir a Claude Code que analice el bug
 *   4. Aplicar el fix sugerido
 *   5. Verificar que todos los tests pasan
 */
class LoanCalculatorServiceTest {

    private final LoanCalculatorService service = new LoanCalculatorService();

    @Test
    @DisplayName("should_calculateCorrectInstallment_when_standardLoan")
    void should_calculateCorrectInstallment_when_standardLoan() {
        // Préstamo $10.000, 24 meses, tasa anual 12%
        // Cuota correcta: $470.73
        BigDecimal amount = new BigDecimal("10000");
        BigDecimal annualRate = new BigDecimal("12"); // 12% anual
        int termMonths = 24;

        BigDecimal result = service.calculateInstallment(amount, termMonths, annualRate);

        assertThat(result)
            .isCloseTo(new BigDecimal("470.73"), within(new BigDecimal("1.00")));
    }

    @Test
    @DisplayName("should_calculateCorrectInstallment_when_highValueLoan")
    void should_calculateCorrectInstallment_when_highValueLoan() {
        // Préstamo $100.000, 120 meses, tasa anual 8.5%
        // Cuota correcta: $1.239.76
        // BUG: el código actual produce un valor significativamente diferente
        BigDecimal amount = new BigDecimal("100000");
        BigDecimal annualRate = new BigDecimal("8.5");
        int termMonths = 120;

        BigDecimal result = service.calculateInstallment(amount, termMonths, annualRate);

        assertThat(result)
            .isCloseTo(new BigDecimal("1239.76"), within(new BigDecimal("0.50")));
    }

    @Test
    @DisplayName("should_returnZero_when_nullAmount")
    void should_returnZero_when_nullAmount() {
        // BUG del código: retorna ZERO pero debería lanzar excepción
        // El alumno debe decidir el comportamiento correcto con Claude Code
        BigDecimal result = service.calculateInstallment(null, 12, new BigDecimal("10"));
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should_calculateTotalCost_when_validLoan")
    void should_calculateTotalCost_when_validLoan() {
        BigDecimal amount = new BigDecimal("10000");
        BigDecimal annualRate = new BigDecimal("12");
        int termMonths = 24;

        BigDecimal total = service.calculateTotalCost(amount, termMonths, annualRate);

        // Total debe ser cuota * 12 meses, entre 11.000 y 11.300
        assertThat(total).isBetween(new BigDecimal("11000"), new BigDecimal("11300"));
    }
}
