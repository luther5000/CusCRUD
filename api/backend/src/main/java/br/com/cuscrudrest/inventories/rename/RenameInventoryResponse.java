package br.com.cuscrudrest.inventories.rename;

/**
 * Payload de saida do endpoint de renomeacao de inventario.
 * Contem os dados do inventario atualizado e a role owner do usuario autenticado.
 * Efeitos colaterais: nenhum.
 *
 * @param inventory dados atualizados do inventario.
 * @param role papel do usuario autenticado no inventario.
 */
public record RenameInventoryResponse(RenameInventoryBodyResponse inventory, int role) {
}
