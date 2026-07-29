package br.com.cuscrudrest.inventories;

import java.util.UUID;

/**
 * Projecao minima de um inventario persistido.
 * Representa o identificador e o nome atual do recurso para leituras internas do dominio.
 * Efeitos colaterais: nenhum.
 *
 * @param inventoryId identificador unico do inventario.
 * @param inventoryName nome persistido do inventario.
 */
public record InventorySummary(UUID inventoryId, String inventoryName) {
}
