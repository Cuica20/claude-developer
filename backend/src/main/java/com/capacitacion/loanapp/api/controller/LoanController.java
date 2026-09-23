package com.capacitacion.loanapp.api.controller;

import com.capacitacion.loanapp.api.dto.ErrorResponse;
import com.capacitacion.loanapp.api.dto.LoanRequest;
import com.capacitacion.loanapp.api.dto.LoanResponse;
import com.capacitacion.loanapp.domain.model.Loan;
import com.capacitacion.loanapp.domain.service.OrderService;
import com.capacitacion.loanapp.domain.validation.LoanEligibilityValidator;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final OrderService orderService;
    private final LoanEligibilityValidator loanEligibilityValidator;

    public LoanController(OrderService orderService, LoanEligibilityValidator loanEligibilityValidator) {
        this.orderService = orderService;
        this.loanEligibilityValidator = loanEligibilityValidator;
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

        Map<String, String> fieldErrors = loanEligibilityValidator.validate(req);
        if (!fieldErrors.isEmpty()) {
            log.warn("Solicitud rechazada por reglas de negocio: {}", fieldErrors.keySet());
            return ResponseEntity.badRequest().body(ErrorResponse.ofFields(400, fieldErrors));
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
