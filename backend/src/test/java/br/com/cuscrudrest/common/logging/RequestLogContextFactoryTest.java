package br.com.cuscrudrest.common.logging;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestLogContextFactoryTest {

    private final RequestLogContextFactory requestLogContextFactory = new RequestLogContextFactory();

    /**
     * Verifica que o IP vindo de X-Forwarded-For tem prioridade e entra no request_id.
     * Entrada: request com cabecalho X-Forwarded-For contendo multiplos IPs.
     * Esperado: uso do primeiro IP como `client_ip` e como prefixo sanitizado do `request_id`.
     */
    @Test
    void shouldUseFirstForwardedIpWhenHeaderIsPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.8, 10.0.0.5");
        request.setRemoteAddr("127.0.0.1");

        RequestLogContext requestLogContext = requestLogContextFactory.create(request);

        assertEquals("203.0.113.8", requestLogContext.clientIp());
        assertTrue(requestLogContext.requestId().startsWith("203_0_113_8-"));
    }

    /**
     * Verifica que o IP remoto e usado quando nao existe X-Forwarded-For.
     * Entrada: request sem cabecalho de proxy e com remoteAddr preenchido.
     * Esperado: uso do remoteAddr como `client_ip` e como prefixo sanitizado do `request_id`.
     */
    @Test
    void shouldFallbackToRemoteAddressWhenForwardedHeaderIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("2001:db8::10");

        RequestLogContext requestLogContext = requestLogContextFactory.create(request);

        assertEquals("2001:db8::10", requestLogContext.clientIp());
        assertTrue(requestLogContext.requestId().startsWith("2001_db8_10-"));
    }
}
