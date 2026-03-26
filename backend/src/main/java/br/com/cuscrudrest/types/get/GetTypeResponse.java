package br.com.cuscrudrest.types.get;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Payload de saida do endpoint de leitura unitaria de tipo.
 * Contem os dados completos expostos pela API para um tipo especifico.
 * Efeitos colaterais: nenhum.
 *
 * @param typeId identificador do tipo.
 * @param nome nome do tipo.
 * @param imagem imagem serializada como data URI, quando houver.
 * @param inventoryId identificador do inventario ao qual o tipo pertence.
 */
public record GetTypeResponse(
        @JsonProperty("type_id")
        long typeId,
        String nome,
        String imagem,
        @JsonProperty("inv_id")
        UUID inventoryId
) {
}
