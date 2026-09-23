package com.capacitacion.loanapp.infrastructure.creditbureau;

import com.capacitacion.loanapp.domain.service.CreditBureauService;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Stub de entrenamiento — NO es una integración real con un buró de crédito.
 * Reemplazar por un cliente HTTP real (con credenciales, timeouts y manejo de errores) antes de
 * cualquier uso en producción.
 *
 * Regla simulada, solo para poder probar ambos caminos de RN-004 sin una BD externa: cualquier
 * solicitante cuyo email termine en "@moroso.test" se reporta con deuda vigente; el resto, sin
 * deuda.
 */
@Component
public class StubCreditBureauService implements CreditBureauService {

    private static final String SIMULATED_DEBTOR_DOMAIN = "@moroso.test";

    @Override
    public CompletableFuture<Boolean> hasActiveDebt(String applicantEmail) {
        boolean hasDebt = applicantEmail != null
            && applicantEmail.toLowerCase().endsWith(SIMULATED_DEBTOR_DOMAIN);
        return CompletableFuture.completedFuture(hasDebt);
    }
}
