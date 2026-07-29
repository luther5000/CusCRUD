package br.com.cuscrudrest.inventories.users.list;

import java.util.List;

/**
 * Resultado interno paginado da listagem de usuarios do inventario.
 * Transporta os dados do inventario, os itens retornados e a continuidade da paginacao.
 * Efeitos colaterais: nenhum.
 *
 * @param inventory inventario consultado.
 * @param users usuarios retornados na pagina atual.
 * @param nextOffset proximo offset, quando houver mais resultados.
 * @param limit limite efetivo usado na consulta.
 */
public record ListInventoryUsersPage(
        ListInventoryUsersInventoryResponse inventory,
        List<ListInventoryUsersItemResponse> users,
        Integer nextOffset,
        int limit
) {
}
