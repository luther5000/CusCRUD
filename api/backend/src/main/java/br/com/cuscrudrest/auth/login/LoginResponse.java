package br.com.cuscrudrest.auth.login;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload de saida do endpoint de login.
 * Contem o token JWT emitido, seu TTL fixo e os dados publicos do usuario autenticado.
 * Efeitos colaterais: nenhum.
 *
 * @param token token JWT HS256 emitido para o usuario.
 * @param expiresIn TTL fixo do token em segundos.
 * @param user dados publicos do usuario autenticado.
 */
public record LoginResponse(
        String token,
        @JsonProperty("expires_in")
        long expiresIn,
        LoginUserResponse user
) {
}
