package br.com.cuscrudrest.auth.jwt;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resultado da validacao de um token JWT.
 * Expõe o usuario autenticado e os metadados temporais extraidos das claims do token.
 * Efeitos colaterais: nenhum.
 *
 * @param userId identificador do usuario autenticado presente na claim `sub`.
 * @param issuedAt instante da claim `iat`.
 * @param expiresAt instante da claim `exp`.
 * @param expiresIn quantidade fixa de segundos entre emissao e expiracao.
 */
public record ValidatedJwtToken(UUID userId, OffsetDateTime issuedAt, OffsetDateTime expiresAt, long expiresIn) {
}
