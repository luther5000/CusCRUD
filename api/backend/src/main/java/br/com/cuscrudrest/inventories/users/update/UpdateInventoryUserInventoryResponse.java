package br.com.cuscrudrest.inventories.users.update;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Representa o inventario retornado na resposta de atualizacao de role de usuario.
 * Efeitos colaterais: nenhum.
 *
 * @param inventoryId identificador do inventario.
 * @param inventoryName nome do inventario.
 */
public record UpdateInventoryUserInventoryResponse(
        @JsonProperty("inv_id")
        UUID inventoryId,
        @JsonProperty("inv_name")
        String inventoryName
) {
}
