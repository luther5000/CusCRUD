package br.com.cuscrudrest.auth.jwt;

import br.com.cuscrudrest.config.CusCrudAuthProperties;
import br.com.cuscrudrest.common.error.UnauthenticatedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String JWT_SECRET = "jwt-secret-for-tests-with-sufficient-length";

    /**
     * Verifica que o servico emite um token JWT HS256 com TTL fixo de 3600 segundos.
     * Entrada: `user_id` valido e relogio fixo.
     * Esperado: token compacto nao vazio, `expires_in = 3600` e metadados temporais coerentes.
     */
    @Test
    void shouldIssueTokenWithFixedTtlAndMetadata() {
        Instant issuedInstant = Instant.parse("2026-03-24T18:00:00Z");
        JwtService jwtService = createJwtService(issuedInstant);
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        IssuedJwtToken issuedToken = jwtService.issueToken(userId);

        assertNotNull(issuedToken.token());
        assertTrue(issuedToken.token().split("\\.").length == 3);
        assertEquals(3600L, issuedToken.expiresIn());
        assertEquals("2026-03-24T15:00-03:00", issuedToken.issuedAt().toString());
        assertEquals("2026-03-24T16:00-03:00", issuedToken.expiresAt().toString());
    }

    /**
     * Verifica que o servico valida um token emitido por ele proprio e extrai as claims esperadas.
     * Entrada: token valido emitido para um `user_id` conhecido.
     * Esperado: `sub`, `iat`, `exp` e `expires_in` coerentes com a especificacao.
     */
    @Test
    void shouldValidateTokenIssuedByService() {
        Instant issuedInstant = Instant.parse("2026-03-24T18:00:00Z");
        JwtService jwtService = createJwtService(issuedInstant);
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        IssuedJwtToken issuedToken = jwtService.issueToken(userId);
        ValidatedJwtToken validatedToken = jwtService.validateToken(issuedToken.token());

        assertEquals(userId, validatedToken.userId());
        assertEquals(3600L, validatedToken.expiresIn());
        assertEquals("2026-03-24T15:00-03:00", validatedToken.issuedAt().toString());
        assertEquals("2026-03-24T16:00-03:00", validatedToken.expiresAt().toString());
    }

    /**
     * Verifica que o servico rejeita token expirado.
     * Entrada: token emitido em um relogio anterior e validado apos o horario de expiracao.
     * Esperado: `UnauthenticatedException`.
     */
    @Test
    void shouldRejectExpiredToken() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        JwtService issueService = createJwtService(Instant.parse("2026-03-24T18:00:00Z"));
        IssuedJwtToken issuedToken = issueService.issueToken(userId);
        JwtService validateService = createJwtService(Instant.parse("2026-03-24T19:00:01Z"));

        assertThrows(UnauthenticatedException.class, () -> validateService.validateToken(issuedToken.token()));
    }

    /**
     * Verifica que o servico rejeita token com assinatura adulterada.
     * Entrada: token valido com o segmento de assinatura modificado.
     * Esperado: `UnauthenticatedException`.
     */
    @Test
    void shouldRejectTokenWithTamperedSignature() {
        JwtService jwtService = createJwtService(Instant.parse("2026-03-24T18:00:00Z"));
        IssuedJwtToken issuedToken = jwtService.issueToken(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

        String[] segments = issuedToken.token().split("\\.");
        String tamperedPayload = flipBase64UrlCharacter(segments[1], 5);
        String tamperedToken = segments[0] + "." + tamperedPayload + "." + segments[2];

        assertThrows(UnauthenticatedException.class, () -> jwtService.validateToken(tamperedToken));
    }

    /**
     * Verifica que o servico extrai corretamente o token bruto do header Authorization.
     * Entrada: header no formato `Bearer <token>`.
     * Esperado: retorno do token sem o prefixo `Bearer `.
     */
    @Test
    void shouldExtractBearerTokenFromAuthorizationHeader() {
        JwtService jwtService = createJwtService(Instant.parse("2026-03-24T18:00:00Z"));

        assertEquals("abc.def.ghi", jwtService.extractBearerToken("Bearer abc.def.ghi"));
    }

    /**
     * Verifica que o servico rejeita header Authorization sem o prefixo Bearer.
     * Entrada: header invalido de autenticacao.
     * Esperado: `UnauthenticatedException`.
     */
    @Test
    void shouldRejectAuthorizationHeaderWithoutBearerPrefix() {
        JwtService jwtService = createJwtService(Instant.parse("2026-03-24T18:00:00Z"));

        assertThrows(UnauthenticatedException.class, () -> jwtService.extractBearerToken("Basic abc.def.ghi"));
    }

    /**
     * Verifica que o header do token emitido declara `alg = HS256` e `typ = JWT`.
     * Entrada: token emitido pelo servico.
     * Esperado: header JSON com os valores exigidos pela especificacao.
     */
    @Test
    void shouldEmitTokenWithExpectedHeader() throws Exception {
        JwtService jwtService = createJwtService(Instant.parse("2026-03-24T18:00:00Z"));
        IssuedJwtToken issuedToken = jwtService.issueToken(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

        String headerSegment = issuedToken.token().split("\\.")[0];
        String headerJson = new String(Base64.getUrlDecoder().decode(headerSegment));
        ObjectMapper objectMapper = new ObjectMapper();

        assertEquals("HS256", objectMapper.readTree(headerJson).path("alg").asText());
        assertEquals("JWT", objectMapper.readTree(headerJson).path("typ").asText());
    }

    /**
     * Cria uma instancia do servico JWT com relogio fixo para teste deterministico.
     *
     * @param now instante atual a ser usado pelo relogio do teste.
     * @return servico JWT configurado com segredo, TTL e clock fixo.
     */
    private JwtService createJwtService(Instant now) {
        CusCrudAuthProperties authProperties = new CusCrudAuthProperties(JWT_SECRET, 3600L);
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        return new JwtService(authProperties, new ObjectMapper(), clock);
    }

    /**
     * Altera o ultimo caractere de uma string Base64URL preservando o alfabeto valido.
     *
     * @param value segmento Base64URL original.
     * @return segmento equivalente, mas com o ultimo caractere alterado.
     */
    private String flipBase64UrlCharacter(String value, int index) {
        char currentChar = value.charAt(index);
        char replacement = currentChar == 'a' ? 'b' : 'a';
        return value.substring(0, index) + replacement + value.substring(index + 1);
    }
}
