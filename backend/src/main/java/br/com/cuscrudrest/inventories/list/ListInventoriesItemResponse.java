package br.com.cuscrudrest.inventories.list;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Item individual da resposta de listagem de inventarios.
 * Expõe os dados publicos do inventario e a role do usuario autenticado naquele recurso.
 * Efeitos colaterais: nenhum.
 *
 * @param inventoryId identificador do inventario.
 * @param inventoryName nome persistido do inventario.
 * @param role role do usuario autenticado no inventario.
 */
public record ListInventoriesItemResponse(
        @JsonProperty("inv_id")
        UUID inventoryId,
        @JsonProperty("inv_name")
        String inventoryName,
        int role
) {
}
