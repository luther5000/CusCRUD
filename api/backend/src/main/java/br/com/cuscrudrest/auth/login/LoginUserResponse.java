package br.com.cuscrudrest.auth.login;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Dados publicos do usuario autenticado retornados no login.
 * Expõe apenas o identificador e os metadados de cadastro, sem incluir a senha.
 * Efeitos colaterais: nenhum.
 *
 * @param userId identificador unico do usuario autenticado.
 * @param name nome persistido do usuario.
 * @param login email persistido do usuario.
 * @param createdAt instante de criacao do cadastro.
 */
public record LoginUserResponse(
        @JsonProperty("user_id")
        UUID userId,
        String name,
        String login,
        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
}
