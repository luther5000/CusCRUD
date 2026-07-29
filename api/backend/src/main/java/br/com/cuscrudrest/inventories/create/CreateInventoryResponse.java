package br.com.cuscrudrest.inventories.create;

/**
 * Payload de saida do endpoint de criacao de inventario.
 * Contem os dados do inventario criado e o papel owner atribuido ao usuario autenticado.
 * Efeitos colaterais: nenhum.
 *
 * @param inventory dados do inventario criado.
 * @param role papel atribuido ao usuario autenticado.
 */
public record CreateInventoryResponse(CreateInventoryBodyResponse inventory, int role) {
}
