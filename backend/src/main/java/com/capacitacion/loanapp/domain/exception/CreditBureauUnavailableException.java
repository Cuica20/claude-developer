package com.capacitacion.loanapp.domain.exception;

/**
 * El buró de crédito falló al responder. No es un rechazo de negocio (no implica que el
 * solicitante tenga o no deuda vigente) sino una falla del sistema — se traduce a 500, no a 400.
 */
public class CreditBureauUnavailableException extends RuntimeException {

    public CreditBureauUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
