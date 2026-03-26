package br.com.cuscrudrest.products;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Projecao resumida de um produto retornado em listagens.
 * Representa o estado persistido necessario para a resposta paginada de produtos.
 * Efeitos colaterais: nenhum.
 *
 * @param productId identificador do produto.
 * @param typeId identificador do tipo associado ao produto.
 * @param marca marca ou fabricante do produto, quando houver.
 * @param dataValidade data de validade do produto com timezone, quando houver.
 * @param unidade unidade base do produto, quando houver.
 * @param unidadeMedida texto da unidade de medida, quando houver.
 * @param quantidade quantidade atual do produto.
 * @param inventoryId identificador do inventario ao qual o produto pertence.
 */
public record ProductSummary(
        long productId,
        long typeId,
        String marca,
        OffsetDateTime dataValidade,
        Long unidade,
        String unidadeMedida,
        long quantidade,
        UUID inventoryId
) {
}
