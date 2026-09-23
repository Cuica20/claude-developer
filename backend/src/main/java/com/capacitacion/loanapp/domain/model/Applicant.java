package com.capacitacion.loanapp.domain.model;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class Applicant {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String dni;
    private LocalDate birthDate;
    private BigDecimal monthlyIncome;
    private int creditScore;
}
