package br.com.cuscrudrest.auth.validate;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidateServiceTest {

    private final ValidateService validateService = new ValidateService();

    /**
     * Verifica que o servico converte o principal autenticado no payload do endpoint.
     * Entrada: principal preenchido pelo modulo de seguranca JWT.
     * Esperado: resposta com os mesmos dados publicos do usuario e metadados do token.
     */
    @Test
    void shouldBuildValidateResponseFromAuthenticatedPrincipal() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-20T10:00:00-03:00");
        OffsetDateTime issuedAt = OffsetDateTime.parse("2026-03-25T09:00:00-03:00");
        OffsetDateTime expiresAt = OffsetDateTime.parse("2026-03-25T10:00:00-03:00");
        UUID userId = UUID.randomUUID();

        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                userId,
                "Joao Novo",
                "joao.novo@example.com",
                createdAt,
                issuedAt,
                expiresAt,
                3600
        );

        ValidateResponse response = validateService.validate(principal);

        assertEquals(userId, response.user().userId());
        assertEquals("Joao Novo", response.user().name());
        assertEquals("joao.novo@example.com", response.user().login());
        assertEquals(createdAt, response.user().createdAt());
        assertEquals(3600, response.token().expiresIn());
        assertEquals(issuedAt, response.token().issuedAt());
    }
}
