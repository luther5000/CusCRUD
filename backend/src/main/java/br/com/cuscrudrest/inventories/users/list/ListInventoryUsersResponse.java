package br.com.cuscrudrest.inventories.users.list;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Payload de saida do endpoint de listagem de usuarios do inventario.
 * Contem o inventario consultado, os usuarios retornados e a URL da proxima pagina, quando houver.
 * Efeitos colaterais: nenhum.
 *
 * @param inventory inventario consultado.
 * @param users usuarios retornados na pagina atual.
 * @param nextPage URL absoluta da proxima pagina, quando houver.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListInventoryUsersResponse(
        ListInventoryUsersInventoryResponse inventory,
        List<ListInventoryUsersItemResponse> users,
        @JsonProperty("next_page")
        String nextPage
) {
}
