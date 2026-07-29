package br.com.cuscrudrest.inventories.users.list;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Dados do inventario retornados na listagem de usuarios com acesso.
 * Expõe o identificador e o nome do inventario consultado.
 * Efeitos colaterais: nenhum.
 *
 * @param inventoryId identificador do inventario.
 * @param inventoryName nome persistido do inventario.
 */
public record ListInventoryUsersInventoryResponse(
        @JsonProperty("inv_id")
        UUID inventoryId,
        @JsonProperty("inv_name")
        String inventoryName
) {
}
