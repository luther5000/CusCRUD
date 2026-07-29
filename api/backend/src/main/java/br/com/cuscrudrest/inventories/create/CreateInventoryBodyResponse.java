package br.com.cuscrudrest.inventories.create;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Dados do inventario criado retornados pela API.
 * Expõe apenas o identificador e o nome persistido do inventario.
 * Efeitos colaterais: nenhum.
 *
 * @param inventoryId identificador unico do inventario.
 * @param inventoryName nome persistido do inventario.
 */
public record CreateInventoryBodyResponse(
        @JsonProperty("inv_id")
        UUID inventoryId,
        @JsonProperty("inv_name")
        String inventoryName
) {
}
