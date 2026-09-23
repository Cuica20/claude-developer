package com.capacitacion.loanapp.api.controller;

import com.capacitacion.loanapp.api.dto.LoanRequest;
import com.capacitacion.loanapp.api.dto.LoanResponse;
import com.capacitacion.loanapp.domain.model.Loan;
import com.capacitacion.loanapp.domain.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO (M2 - Reglas de Negocio): Las validaciones de negocio están
 * mezcladas directamente en el controlador. Deben extraerse a validadores.
 *
 * Reglas a implementar como clases separadas (ver BUSINESS_RULES.md):
 *  - RN-001 → AgeValidator
 *  - RN-002 → IncomeRatioValidator
 *  - RN-003 → CreditScoreValidator
 *
 * Patrón recomendado: Strategy + LoanEligibilityValidator que orqueste todo.
 */
@Slf4j
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final OrderService orderService;

    public LoanController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<LoanResponse> getAll() {
        return orderService.getAllLoans().stream()
            .map(LoanResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public LoanResponse getById(@PathVariable Long id) {
        return LoanResponse.from(orderService.getById(id));
    }

    @PostMapping
    public ResponseEntity<?> createLoan(@Valid @RequestBody LoanRequest req) {

        // TODO (M2): mover estas validaciones a clases Validator separadas

        Map<String, String> errors = new HashMap<>();

        // RN-001: edad mínima 18 años (inline — mal patrón)
        int age = Period.between(req.birthDate(), LocalDate.now()).getYears();
        if (age < 18) {
            errors.put("birthDate", "RN-001: El solicitante debe ser mayor de 18 años");
        }

        // RN-002: ingreso >= cuota * 3 (inline — sin reutilización)
        BigDecimal cuota = req.amount().divide(BigDecimal.valueOf(req.termMonths()), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal minIncome = cuota.multiply(BigDecimal.valueOf(3));
        if (req.monthlyIncome().compareTo(minIncome) < 0) {
            errors.put("monthlyIncome",
                "RN-002: Ingreso insuficiente. Minimo requerido: " + minIncome);
        }

        // RN-003: score crediticio según monto (inline — hardcodeado)
        int minScore = req.amount().compareTo(BigDecimal.valueOf(50_000)) > 0 ? 700 : 600;
        if (req.creditScore() < minScore) {
            errors.put("creditScore",
                "RN-003: Score insuficiente. Minimo: " + minScore);
        }

        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("fieldErrors", errors));
        }

        Loan loan = Loan.builder()
            .applicantName(req.applicantName())
            .applicantEmail(req.applicantEmail())
            .birthDate(req.birthDate())
            .monthlyIncome(req.monthlyIncome())
            .creditScore(req.creditScore())
            .amount(req.amount())
            .termMonths(req.termMonths())
            .build();

        Loan saved = orderService.processOrder(loan);
        log.info("Nueva solicitud creada: id={}, solicitante={}", saved.getId(), saved.getApplicantName());
        return ResponseEntity.status(201).body(LoanResponse.from(saved));
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return Map.of(
            "total",    orderService.getAllLoans().size(),
            "pending",  orderService.countByStatus(Loan.LoanStatus.PENDING),
            "approved", orderService.countByStatus(Loan.LoanStatus.APPROVED),
            "rejected", orderService.countByStatus(Loan.LoanStatus.REJECTED),
            "portfolio", orderService.calculateTotalPortfolio()
        );
    }
}
