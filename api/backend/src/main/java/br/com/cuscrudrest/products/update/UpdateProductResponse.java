package br.com.cuscrudrest.products.update;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload de saida do endpoint de atualizacao parcial de produtos.
 * Representa o estado persistido do produto apos a aplicacao do patch.
 * Efeitos colaterais: nenhum.
 *
 * @param productId identificador do produto atualizado.
 * @param typeId identificador do tipo associado ao produto apos a atualizacao.
 * @param marca marca ou fabricante persistidos, quando houver.
 * @param dataValidade data de validade persistida com timezone, quando houver.
 * @param unidade unidade base persistida, quando houver.
 * @param unidadeMedida texto da unidade de medida persistido, quando houver.
 * @param quantidade quantidade persistida do produto.
 * @param inventoryId identificador do inventario ao qual o produto pertence.
 */
public record UpdateProductResponse(
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
