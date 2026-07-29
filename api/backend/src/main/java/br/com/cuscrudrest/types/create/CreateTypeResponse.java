package br.com.cuscrudrest.types.create;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Resposta HTTP do endpoint de criacao de tipos.
 * Expoe o identificador gerado e os dados principais do tipo persistido.
 * Efeitos colaterais: nenhum.
 *
 * @param typeId identificador gerado do tipo.
 * @param nome nome do tipo criado.
 * @param imagem imagem do tipo no formato data URI, quando informada na criacao.
 * @param inventoryId identificador do inventario ao qual o tipo pertence.
 */
public record CreateTypeResponse(
        @JsonProperty("type_id")
        long typeId,
        String nome,
        String imagem,
        @JsonProperty("inv_id")
        UUID inventoryId
) {
}
