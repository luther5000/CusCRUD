package br.com.cuscrudrest.inventories.rename;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Dados do inventario renomeado retornados pela API.
 * Expõe apenas o identificador e o novo nome persistido do inventario.
 * Efeitos colaterais: nenhum.
 *
 * @param inventoryId identificador unico do inventario.
 * @param inventoryName nome persistido apos a renomeacao.
 */
public record RenameInventoryBodyResponse(
        @JsonProperty("inv_id")
        UUID inventoryId,
        @JsonProperty("inv_name")
        String inventoryName
) {
}
