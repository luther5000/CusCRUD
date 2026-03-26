package br.com.cuscrudrest.products.list;

import java.util.List;

/**
 * Resultado interno da listagem paginada de produtos.
 * Carrega os itens da pagina, o proximo offset e o limite efetivo utilizado.
 * Efeitos colaterais: nenhum.
 *
 * @param products produtos retornados na pagina atual.
 * @param nextOffset proximo offset, quando houver.
 * @param limit limite efetivo utilizado na consulta.
 */
public record ListProductsPage(
        List<ListProductsItemResponse> products,
        Integer nextOffset,
        int limit
) {
}
