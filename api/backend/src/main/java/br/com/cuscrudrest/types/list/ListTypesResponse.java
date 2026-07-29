package br.com.cuscrudrest.types.list;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Payload de saida do endpoint de listagem de tipos.
 * Contem os tipos do inventario e a URL da proxima pagina, quando houver.
 * Efeitos colaterais: nenhum.
 *
 * @param types tipos retornados na pagina atual.
 * @param nextPage URL absoluta da proxima pagina, quando houver.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListTypesResponse(
        List<ListTypesItemResponse> types,
        @JsonProperty("next_page")
        String nextPage
) {
}
