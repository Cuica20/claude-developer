package com.capacitacion.loanapp.domain.service;

import com.capacitacion.loanapp.domain.model.Loan;
import com.capacitacion.loanapp.infrastructure.persistence.LoanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * TODO (M1 - Refactorización): Esta clase viola el principio SRP.
 *
 * Responsabilidades mezcladas (extraer a clases separadas):
 *  - Validación    → OrderValidator
 *  - Cálculo cuota → InstallmentCalculator
 *  - Persistencia  → delegar a LoanRepository (ya existe)
 *  - Notificación  → OrderNotifier
 *  - Informes      → LoanReportService
 *
 * Problemas adicionales:
 *  - @Autowired en campo (debe ser inyección por constructor)
 *  - processOrder() supera las 50 líneas
 *  - Lógica de tasas hardcodeada
 */
@Slf4j
@Service
public class OrderService {

    // TODO (M1): cambiar a inyección por constructor
    @Autowired
    private LoanRepository loanRepository;

    public Loan processOrder(Loan loan) {

        // ── 1. VALIDACIÓN (debería estar en OrderValidator) ──────────
        if (loan.getAmount() == null || loan.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        if (loan.getTermMonths() == null || loan.getTermMonths() < 6 || loan.getTermMonths() > 360) {
            throw new IllegalArgumentException("El plazo debe estar entre 6 y 360 meses");
        }
        if (loan.getMonthlyIncome() == null || loan.getMonthlyIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El ingreso mensual debe ser mayor a cero");
        }

        // ── 2. CÁLCULO DE CUOTA (debería estar en InstallmentCalculator) ──
        BigDecimal monthlyRate;
        if (loan.getAmount().compareTo(new BigDecimal("50000")) > 0) {
            monthlyRate = new BigDecimal("0.0085");
        } else if (loan.getAmount().compareTo(new BigDecimal("20000")) > 0) {
            monthlyRate = new BigDecimal("0.0095");
        } else {
            monthlyRate = new BigDecimal("0.0110");
        }
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        double pow = Math.pow(onePlusR.doubleValue(), -loan.getTermMonths());
        BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.valueOf(pow));
        BigDecimal installment = loan.getAmount()
            .multiply(monthlyRate)
            .divide(denominator, 2, RoundingMode.HALF_UP);
        loan.setMonthlyInstallment(installment);

        // ── 3. PERSISTENCIA ──────────────────────────────────────────
        loan.setStatus(Loan.LoanStatus.PENDING);
        loan.setCreatedAt(LocalDateTime.now());
        Loan saved = loanRepository.save(loan);
        log.info("Prestamo #{} guardado. Cuota mensual: {}", saved.getId(), installment);

        // ── 4. NOTIFICACION (debería estar en OrderNotifier) ─────────
        sendConfirmationEmail(saved);

        return saved;
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public Loan getById(Long id) {
        return loanRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Prestamo no encontrado: " + id));
    }

    // ── 5. INFORME (debería estar en LoanReportService) ──────────
    public BigDecimal calculateTotalPortfolio() {
        return loanRepository.findAll().stream()
            .map(Loan::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long countByStatus(Loan.LoanStatus status) {
        return loanRepository.findByStatusOrderByCreatedAtDesc(status).size();
    }

    // TODO (M1): extraer a OrderNotifier inyectado
    private void sendConfirmationEmail(Loan loan) {
        log.info("[SIMULADO] Email enviado a {} - Solicitud #{} recibida. Cuota: {}",
            loan.getApplicantEmail(), loan.getId(), loan.getMonthlyInstallment());
    }
}
