package br.com.cuscrudrest.inventories;

import java.util.UUID;

/**
 * Contexto de acesso do usuario autenticado a um inventario.
 * Carrega o recurso resolvido e a role do usuario para reutilizacao pelos casos de uso protegidos.
 * Efeitos colaterais: nenhum.
 *
 * @param inventoryId identificador do inventario validado.
 * @param inventoryName nome atual do inventario.
 * @param role role do usuario autenticado no inventario.
 */
public record InventoryAccessContext(UUID inventoryId, String inventoryName, int role) {
}
