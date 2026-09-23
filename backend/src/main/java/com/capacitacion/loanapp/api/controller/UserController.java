package com.capacitacion.loanapp.api.controller;

import com.capacitacion.loanapp.api.dto.LoanResponse;
import com.capacitacion.loanapp.domain.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TODO (M8 - Code Review): Este controlador tiene vulnerabilidades
 * que deben detectarse en code review antes de mergear a main.
 *
 * Problemas a detectar con Claude Code:
 *  1. IDOR (Insecure Direct Object Reference): /api/users/{userId}/loans
 *     no verifica que el usuario autenticado sea el dueño de los préstamos
 *  2. Exposición de datos: devuelve TODOS los campos del préstamo incluyendo
 *     datos sensibles (ingresos, score crediticio) sin filtrar por rol
 *  3. Sin paginación: getLoansForUser puede retornar miles de registros
 *  4. Logging de datos sensibles: loguea el email del usuario en INFO
 *  5. Sin @PreAuthorize: cualquier usuario puede consultar datos de cualquier otro
 *
 * Flujo de code review con Claude Code:
 *   git diff main feature/user-loans | claude
 *     "revisa este diff para: OWASP Top 10, convenciones de CLAUDE.md,
 *      problemas de performance y exposición de datos sensibles.
 *      Lista los issues con severidad: CRÍTICO / ALTO / MEDIO / BAJO"
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final OrderService orderService;

    public UserController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * IDOR: no verifica que userId == usuarioAutenticado
     * Cualquier usuario puede ver los préstamos de cualquier otro.
     */
    @GetMapping("/{userId}/loans")
    public List<LoanResponse> getLoansForUser(@PathVariable Long userId,
                                               @RequestParam(required = false) String email) {
        // TODO (M8): verificar que SecurityContextHolder.getContext().getAuthentication()
        //            corresponde al userId antes de responder
        log.info("Consultando préstamos para userId={}, email={}", userId, email); // BUG: loguea email

        if (email != null) {
            // BUG: endpoint alternativo sin auth — solo con conocer el email
            return orderService.getAllLoans().stream()
                .filter(l -> email.equals(l.getApplicantEmail()))
                .map(LoanResponse::from)
                .toList();
        }

        // BUG: sin paginación, retorna todos los préstamos del sistema si userId no filtra
        return orderService.getAllLoans().stream()
            .map(LoanResponse::from)
            .toList();
    }
}
