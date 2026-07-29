package br.com.cuscrudrest.inventories.users.list;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Item individual da resposta de listagem de usuarios do inventario.
 * Expõe os dados publicos do usuario e sua role no inventario.
 * Efeitos colaterais: nenhum.
 *
 * @param userId identificador do usuario.
 * @param name nome persistido do usuario.
 * @param login email persistido do usuario.
 * @param role role do usuario no inventario.
 */
public record ListInventoryUsersItemResponse(
        @JsonProperty("user_id")
        UUID userId,
        String name,
        String login,
        int role
) {
}
