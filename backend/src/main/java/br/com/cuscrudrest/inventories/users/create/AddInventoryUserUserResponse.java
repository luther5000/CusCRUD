package br.com.cuscrudrest.inventories.users.create;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Representa o usuario retornado na resposta de adicao ao inventario.
 * Efeitos colaterais: nenhum.
 *
 * @param userId identificador do usuario.
 * @param name nome do usuario.
 * @param login email do usuario.
 * @param role role atribuida no inventario.
 */
public record AddInventoryUserUserResponse(
        @JsonProperty("user_id")
        UUID userId,
        String name,
        String login,
        int role
) {
}
