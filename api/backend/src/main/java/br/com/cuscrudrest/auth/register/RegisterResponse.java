package br.com.cuscrudrest.auth.register;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload de saida do endpoint de cadastro de usuario.
 * Expõe apenas os dados publicos do usuario criado, sem incluir a senha.
 * Efeitos colaterais: nenhum.
 *
 * @param userId identificador unico do usuario criado.
 * @param name nome persistido do usuario.
 * @param login email persistido do usuario.
 * @param createdAt instante de criacao do cadastro.
 */
public record RegisterResponse(
        @JsonProperty("user_id")
        UUID userId,
        String name,
        String login,
        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
}
