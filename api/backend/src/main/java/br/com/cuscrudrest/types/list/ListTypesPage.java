package br.com.cuscrudrest.types.list;

import java.util.List;

/**
 * Representa uma pagina de tipos retornada pela camada de servico.
 * Mantem os itens da pagina, o proximo offset e o limite efetivo usado.
 * Efeitos colaterais: nenhum.
 *
 * @param types tipos retornados na pagina atual.
 * @param nextOffset proximo offset, quando houver.
 * @param limit limite efetivo usado na consulta.
 */
public record ListTypesPage(
        List<ListTypesItemResponse> types,
        Integer nextOffset,
        int limit
) {
}
