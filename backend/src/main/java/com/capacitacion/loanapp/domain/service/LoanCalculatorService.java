package com.capacitacion.loanapp.domain.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

/**
 * TODO (M7 - Debugging): Esta clase tiene un bug sutil de precisión.
 *
 * Problemas a encontrar con Claude Code:
 *  1. usa double para cálculo de interés → pérdida de precisión en valores grandes
 *  2. la tasa de interés se aplica al revés (1 + rate en vez de rate / 100)
 *  3. el resultado se redondea con CEILING en vez de HALF_UP → sobrecobro al cliente
 *  4. no hay validación de plazo negativo ni monto cero
 *
 * Síntoma: para un préstamo de $100.000 a 120 meses la cuota calculada
 * difiere en ~$15 respecto al valor correcto. En producción esto genera
 * reclamos de clientes y discrepancias en el portafolio total.
 *
 * Flujo de debugging con Claude Code:
 *   1. mvn test -Dtest=LoanCalculatorServiceTest  → falla en testHighValueLoan
 *   2. cat stacktrace.txt | claude "analiza este fallo y busca el bug"
 *   3. Claude identifica el double y el CEILING
 *   4. Pedir a Claude el fix + test de regresión
 */
@Service
public class LoanCalculatorService {

    /**
     * Calcula la cuota mensual usando la fórmula de amortización francesa.
     * BUG: usa double internamente — pierde precisión con montos altos.
     */
    public BigDecimal calculateInstallment(BigDecimal amount, int termMonths, BigDecimal annualRate) {
        if (amount == null || termMonths <= 0) {
            return BigDecimal.ZERO; // BUG: debería lanzar IllegalArgumentException
        }

        // BUG 1: conversión a double pierde precisión con montos > $10.000
        double principal = amount.doubleValue();
        double rate = annualRate.doubleValue(); // BUG 2: no divide entre 1200 para obtener tasa mensual

        // Fórmula: PMT = P * r / (1 - (1+r)^-n)
        // BUG 3: 'rate' aquí debería ser mensual (annualRate / 1200)
        double monthlyRate = rate / 12; // BUG: divide entre 12 pero rate ya viene como 0.12 no como 12
        double pow = Math.pow(1 + monthlyRate, -termMonths);
        double installment = principal * monthlyRate / (1 - pow);

        // BUG 4: CEILING en vez de HALF_UP → siempre redondea arriba (sobrecobro)
        return BigDecimal.valueOf(installment)
            .setScale(2, java.math.RoundingMode.CEILING);
    }

    /**
     * Calcula el costo total del préstamo.
     * BUG: usa calculateInstallment que ya tiene errores.
     */
    public BigDecimal calculateTotalCost(BigDecimal amount, int termMonths, BigDecimal annualRate) {
        BigDecimal installment = calculateInstallment(amount, termMonths, annualRate);
        return installment.multiply(BigDecimal.valueOf(termMonths));
    }
}
