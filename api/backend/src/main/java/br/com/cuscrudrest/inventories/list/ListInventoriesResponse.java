package br.com.cuscrudrest.inventories.list;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Payload de saida do endpoint de listagem de inventarios.
 * Contem os inventarios acessiveis pelo usuario autenticado e a URL da proxima pagina, quando houver.
 * Efeitos colaterais: nenhum.
 *
 * @param inventories inventarios retornados na pagina atual.
 * @param nextPage URL absoluta da proxima pagina, quando houver.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListInventoriesResponse(
        List<ListInventoriesItemResponse> inventories,
        @JsonProperty("next_page")
        String nextPage
) {
}
