package com.capacitacion.loanapp.api.controller;

import com.capacitacion.loanapp.domain.model.Loan;
import com.capacitacion.loanapp.domain.service.OrderService;
import com.capacitacion.loanapp.domain.validation.LoanEligibilityValidator;
import com.capacitacion.loanapp.infrastructure.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
@Import(SecurityConfig.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private LoanEligibilityValidator loanEligibilityValidator;

    private Map<String, Object> validRequestBody() {
        return Map.of(
            "applicantName", "María García",
            "applicantEmail", "maria@test.com",
            "birthDate", "1990-05-01",
            "monthlyIncome", 3000,
            "creditScore", 700,
            "amount", 15000,
            "termMonths", 36);
    }

    @Test
    void should_return400WithAllFieldErrors_when_businessRulesFail() throws Exception {
        Map<String, String> fieldErrors = Map.of(
            "birthDate", "RN-001: El solicitante debe ser mayor de 18 años",
            "monthlyIncome", "RN-002: Ingreso insuficiente. Minimo requerido: 5000.00",
            "creditScore", "RN-003: Score insuficiente. Minimo: 700",
            "applicantEmail", "RN-004: El solicitante registra deuda vigente en el buró de crédito");
        when(loanEligibilityValidator.validate(any())).thenReturn(fieldErrors);

        mockMvc.perform(post("/api/loans")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(validRequestBody())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("ValidationError"))
            .andExpect(jsonPath("$.fieldErrors.birthDate").exists())
            .andExpect(jsonPath("$.fieldErrors.monthlyIncome").exists())
            .andExpect(jsonPath("$.fieldErrors.creditScore").exists())
            .andExpect(jsonPath("$.fieldErrors.applicantEmail").exists());
    }

    @Test
    void should_return201_when_dataIsValid() throws Exception {
        when(loanEligibilityValidator.validate(any())).thenReturn(Map.of());
        Loan saved = Loan.builder()
            .id(1L)
            .applicantName("María García")
            .applicantEmail("maria@test.com")
            .birthDate(LocalDate.of(1990, 5, 1))
            .monthlyIncome(BigDecimal.valueOf(3000))
            .creditScore(700)
            .amount(BigDecimal.valueOf(15000))
            .termMonths(36)
            .monthlyInstallment(BigDecimal.valueOf(500))
            .status(Loan.LoanStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
        when(orderService.processOrder(any())).thenReturn(saved);

        mockMvc.perform(post("/api/loans")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(validRequestBody())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
