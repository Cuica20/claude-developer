package com.capacitacion.loanapp.api.dto;

import com.capacitacion.loanapp.domain.model.Loan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoanResponse(
    Long id,
    String applicantName,
    String applicantEmail,
    LocalDate birthDate,
    BigDecimal monthlyIncome,
    Integer creditScore,
    BigDecimal amount,
    Integer termMonths,
    BigDecimal monthlyInstallment,
    String status,
    LocalDateTime createdAt,
    String notes
) {
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
            loan.getId(),
            loan.getApplicantName(),
            loan.getApplicantEmail(),
            loan.getBirthDate(),
            loan.getMonthlyIncome(),
            loan.getCreditScore(),
            loan.getAmount(),
            loan.getTermMonths(),
            loan.getMonthlyInstallment(),
            loan.getStatus().name(),
            loan.getCreatedAt(),
            loan.getNotes()
        );
    }
}
