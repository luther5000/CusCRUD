package br.com.cuscrudrest.products.get;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload de saida do endpoint de leitura unitaria de produto.
 * Contem os dados completos expostos pela API para um produto especifico.
 * Efeitos colaterais: nenhum.
 *
 * @param productId identificador do produto.
 * @param typeId identificador do tipo associado ao produto.
 * @param marca marca ou fabricante, quando houver.
 * @param dataValidade data de validade com timezone, quando houver.
 * @param unidade unidade base do produto, quando houver.
 * @param unidadeMedida texto da unidade de medida, quando houver.
 * @param quantidade quantidade atual do produto.
 * @param inventoryId identificador do inventario ao qual o produto pertence.
 */
public record GetProductResponse(
        @JsonProperty("product_id")
        long productId,
        @JsonProperty("type_id")
        long typeId,
        String marca,
        OffsetDateTime dataValidade,
        Long unidade,
        String unidadeMedida,
        long quantidade,
        @JsonProperty("inv_id")
        UUID inventoryId
) {
}
