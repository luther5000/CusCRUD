package br.com.cuscrudrest.auth;

import java.time.OffsetDateTime;

/**
 * Resultado da emissao de um token JWT.
 * Transporta o token assinado e seus metadados temporais ja convertidos para a timezone da aplicacao.
 * Efeitos colaterais: nenhum.
 *
 * @param token token JWT assinado em formato compacto.
 * @param expiresIn quantidade fixa de segundos ate a expiracao.
 * @param issuedAt instante de emissao do token.
 * @param expiresAt instante de expiracao do token.
 */
public record IssuedJwtToken(String token, long expiresIn, OffsetDateTime issuedAt, OffsetDateTime expiresAt) {
}
