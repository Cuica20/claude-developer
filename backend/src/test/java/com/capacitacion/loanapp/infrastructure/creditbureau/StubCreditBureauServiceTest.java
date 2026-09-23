package com.capacitacion.loanapp.infrastructure.creditbureau;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StubCreditBureauServiceTest {

    private final StubCreditBureauService service = new StubCreditBureauService();

    @Test
    void should_reportActiveDebt_when_emailEndsInMorosoTestDomain() throws Exception {
        boolean hasDebt = service.hasActiveDebt("juan@moroso.test").get();

        assertTrue(hasDebt);
    }

    @Test
    void should_reportNoDebt_when_emailIsAnyOtherDomain() throws Exception {
        boolean hasDebt = service.hasActiveDebt("juan@gmail.com").get();

        assertFalse(hasDebt);
    }
}
