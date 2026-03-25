package br.com.cuscrudrest.inventories;

import java.util.UUID;

/**
 * Projecao de um inventario acessivel por um usuario autenticado.
 * Representa os dados minimos retornados nas listagens com a role do usuario no recurso.
 * Efeitos colaterais: nenhum.
 *
 * @param inventoryId identificador unico do inventario.
 * @param inventoryName nome persistido do inventario.
 * @param role role do usuario autenticado no inventario.
 */
public record UserInventorySummary(UUID inventoryId, String inventoryName, int role) {
}
