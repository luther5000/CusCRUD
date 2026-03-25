package br.com.cuscrudrest.auth.validate;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Dados publicos do usuario retornados pelo endpoint de validacao.
 * Expõe apenas os campos seguros de identificacao e cadastro do usuario autenticado.
 * Efeitos colaterais: nenhum.
 *
 * @param userId identificador unico do usuario autenticado.
 * @param name nome persistido do usuario.
 * @param login email persistido do usuario.
 * @param createdAt instante de criacao do cadastro.
 */
public record ValidateUserResponse(
        @JsonProperty("user_id")
        UUID userId,
        String name,
        String login,
        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
}
