package com.capacitacion.loanapp.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos del solicitante
    private String applicantName;
    private String applicantEmail;
    private LocalDate birthDate;
    private BigDecimal monthlyIncome;
    private Integer creditScore;

    // Datos del préstamo
    private BigDecimal amount;
    private Integer termMonths;
    private BigDecimal monthlyInstallment;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LoanStatus status = LoanStatus.PENDING;

    private LocalDateTime createdAt;
    private String notes;

    public enum LoanStatus {
        PENDING, APPROVED, REJECTED, DISBURSED, IN_MORA
    }
}
