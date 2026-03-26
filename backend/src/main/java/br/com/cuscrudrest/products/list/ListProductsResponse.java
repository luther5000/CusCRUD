package br.com.cuscrudrest.products.list;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Payload de saida do endpoint de listagem de produtos.
 * Contem os produtos do inventario e a URL da proxima pagina, quando houver.
 * Efeitos colaterais: nenhum.
 *
 * @param products produtos retornados na pagina atual.
 * @param nextPage URL absoluta da proxima pagina, quando houver.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListProductsResponse(
        List<ListProductsItemResponse> products,
        @JsonProperty("next_page")
        String nextPage
) {
}
