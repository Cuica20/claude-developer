package com.capacitacion.loanapp.domain.service;

import com.capacitacion.loanapp.domain.model.Loan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

/**
 * TODO (M3 - Testing): Estos tests son incompletos.
 * - No mockean el JavaMailSender (llaman al servidor real)
 * - Solo prueban el happy path
 * - No hay verificación de casos borde
 * - No hay @ExtendWith(MockitoExtension.class)
 * Usar Claude Code para regenerarlos desde TEST_SPEC.md
 */
@SpringBootTest  // TODO: reemplazar con unit test puro + @ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Autowired
    private OrderService orderService;  // TODO: instanciar manualmente con mocks

    @Test
    void processOrder_shouldWork() {
        // TODO: este test no verifica nada útil
        Loan loan = new Loan();
        loan.setAmount(new BigDecimal("10000"));
        loan.setTermMonths(24);
        // orderService.processOrder(loan); // comentado porque falla sin mailSender
    }

    // TODO: agregar tests para:
    // - monto nulo → RuntimeException
    // - monto negativo → RuntimeException
    // - plazo 0 → RuntimeException
    // - plazo 361 → RuntimeException
    // - monto > 50000 → descuento 2%
    // - monto entre 10001 y 50000 → descuento 1%
    // - verificar que se llama a mailSender.send()
}
