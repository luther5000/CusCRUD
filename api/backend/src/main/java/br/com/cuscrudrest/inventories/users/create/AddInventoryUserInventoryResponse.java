package br.com.cuscrudrest.inventories.users.create;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Representa o inventario retornado na resposta de adicao de usuario.
 * Efeitos colaterais: nenhum.
 *
 * @param inventoryId identificador do inventario.
 * @param inventoryName nome do inventario.
 */
public record AddInventoryUserInventoryResponse(
        @JsonProperty("inv_id")
        UUID inventoryId,
        @JsonProperty("inv_name")
        String inventoryName
) {
}
