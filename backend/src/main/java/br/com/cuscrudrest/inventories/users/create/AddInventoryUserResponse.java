package br.com.cuscrudrest.inventories.users.create;

/**
 * Resposta HTTP do endpoint de adicao de usuario ao inventario.
 * Contem o inventario alvo e os dados publicos do usuario com a role atribuida.
 * Efeitos colaterais: nenhum.
 *
 * @param inventory inventario ao qual o usuario foi vinculado.
 * @param user usuario adicionado ao inventario.
 */
public record AddInventoryUserResponse(
        AddInventoryUserInventoryResponse inventory,
        AddInventoryUserUserResponse user
) {
}
