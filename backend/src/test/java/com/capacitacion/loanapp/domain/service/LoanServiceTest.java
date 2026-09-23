package com.capacitacion.loanapp.domain.service;

import com.capacitacion.loanapp.domain.model.Loan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO (M3 - Testing Inteligente): Estos tests son incompletos.
 *
 * Problemas detectados:
 *  1. @SpringBootTest carga TODO el contexto (lento y frágil para unit tests)
 *  2. No se mockea el repositorio — escribe en la BD real de test
 *  3. Solo existe el happy path — sin casos borde ni errores
 *  4. No hay @ExtendWith(MockitoExtension.class)
 *  5. No se verifica comportamiento: ningún assert sobre cuota calculada
 *
 * Ver TEST_SPEC.md para la especificación completa.
 * Usar Claude Code para regenerar con mocks y cobertura completa.
 */
@SpringBootTest
@Transactional
class LoanServiceTest {

    @Autowired
    private OrderService orderService;   // TODO: instanciar manualmente con new OrderService(mockRepo)

    @Test
    void should_processOrder_when_validData() {
        // TODO: este test no verifica nada útil — solo que no lanza excepción
        Loan loan = Loan.builder()
            .applicantName("Test User")
            .applicantEmail("test@test.com")
            .amount(new BigDecimal("10000"))
            .termMonths(24)
            .build();

        Loan result = orderService.processOrder(loan);

        assertNotNull(result.getId());
        // TODO: verificar que monthlyInstallment fue calculado correctamente
        // TODO: verificar que status es PENDING
        // TODO: verificar que se simuló la notificación (spy/mock)
    }

    // TODO: agregar tests para:
    // should_throwException_when_amountIsNull
    // should_throwException_when_amountIsNegative
    // should_throwException_when_termIsZero
    // should_throwException_when_termExceeds360
    // should_applyHighRate_when_amountBelow20k
    // should_applyMediumRate_when_amountBetween20kAnd50k
    // should_applyLowRate_when_amountAbove50k
}
