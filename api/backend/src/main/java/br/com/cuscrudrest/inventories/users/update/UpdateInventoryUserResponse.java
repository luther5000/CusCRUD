package br.com.cuscrudrest.inventories.users.update;

/**
 * Resposta HTTP do endpoint de atualizacao da role de um usuario no inventario.
 * Contem o inventario alvo e os dados publicos do usuario com a nova role.
 * Efeitos colaterais: nenhum.
 *
 * @param inventory inventario no qual a role foi alterada.
 * @param user usuario atualizado.
 */
public record UpdateInventoryUserResponse(
        UpdateInventoryUserInventoryResponse inventory,
        UpdateInventoryUserUserResponse user
) {
}
