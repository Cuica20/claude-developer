package com.capacitacion.loanapp.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanRequest(

    @NotBlank(message = "El nombre es obligatorio")
    String applicantName,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    String applicantEmail,

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha debe ser en el pasado")
    LocalDate birthDate,

    @NotNull(message = "El ingreso mensual es obligatorio")
    @Positive(message = "El ingreso debe ser positivo")
    BigDecimal monthlyIncome,

    @NotNull(message = "El score crediticio es obligatorio")
    @Min(value = 300, message = "El score mínimo es 300")
    @Max(value = 850, message = "El score máximo es 850")
    Integer creditScore,

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "1000.00", message = "El monto mínimo es 1.000")
    BigDecimal amount,

    @NotNull(message = "El plazo es obligatorio")
    @Min(value = 6,   message = "El plazo mínimo es 6 meses")
    @Max(value = 360, message = "El plazo máximo es 360 meses")
    Integer termMonths
) {}
