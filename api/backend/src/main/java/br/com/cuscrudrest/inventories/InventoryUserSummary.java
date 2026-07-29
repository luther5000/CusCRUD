package br.com.cuscrudrest.inventories;

import java.util.UUID;

/**
 * Projecao de um usuario com acesso a um inventario.
 * Representa os dados publicos do usuario e a role associada ao inventario consultado.
 * Efeitos colaterais: nenhum.
 *
 * @param userId identificador unico do usuario.
 * @param name nome persistido do usuario.
 * @param login email persistido do usuario.
 * @param role role do usuario no inventario.
 */
public record InventoryUserSummary(UUID userId, String name, String login, int role) {
}
