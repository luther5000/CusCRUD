package br.com.cuscrudrest.inventories.list;

import java.util.List;

/**
 * Resultado interno paginado da listagem de inventarios.
 * Transporta os itens retornados e, quando aplicavel, o proximo offset para montagem de `next_page`.
 * Efeitos colaterais: nenhum.
 *
 * @param inventories itens da pagina atual.
 * @param nextOffset proximo offset, quando houver mais resultados.
 * @param limit limite efetivo usado na consulta.
 */
public record ListInventoriesPage(
        List<ListInventoriesItemResponse> inventories,
        Integer nextOffset,
        int limit
) {
}
