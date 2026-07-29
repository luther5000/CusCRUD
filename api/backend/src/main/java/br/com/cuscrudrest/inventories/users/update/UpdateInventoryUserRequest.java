package br.com.cuscrudrest.inventories.users.update;

import jakarta.validation.constraints.NotNull;

/**
 * Payload de entrada para atualizacao da role de um usuario em um inventario.
 * Representa apenas a nova role permitida pela API.
 * Efeitos colaterais: nenhum.
 *
 * @param role nova role do usuario no inventario.
 */
public record UpdateInventoryUserRequest(
        @NotNull(message = "must not be null")
        Integer role
) {
}
