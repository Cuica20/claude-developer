package com.capacitacion.loanapp.domain.service;

import java.util.concurrent.CompletableFuture;

/**
 * Puerto hacia un buró de crédito externo. La implementación real (llamada HTTP, credenciales,
 * timeouts) vive en infrastructure/ — el dominio solo conoce esta interfaz.
 */
public interface CreditBureauService {
    CompletableFuture<Boolean> hasActiveDebt(String applicantEmail);
}
