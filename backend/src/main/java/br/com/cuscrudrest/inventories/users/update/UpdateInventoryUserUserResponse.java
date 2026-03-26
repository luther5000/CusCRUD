package br.com.cuscrudrest.inventories.users.update;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Representa o usuario retornado na resposta de atualizacao de role no inventario.
 * Efeitos colaterais: nenhum.
 *
 * @param userId identificador do usuario.
 * @param name nome do usuario.
 * @param login email do usuario.
 * @param role nova role atribuida no inventario.
 */
public record UpdateInventoryUserUserResponse(
        @JsonProperty("user_id")
        UUID userId,
        String name,
        String login,
        int role
) {
}
