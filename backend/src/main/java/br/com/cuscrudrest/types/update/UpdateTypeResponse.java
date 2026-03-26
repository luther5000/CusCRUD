package br.com.cuscrudrest.types.update;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Resposta HTTP do endpoint de atualizacao de tipos.
 * Expoe o estado final persistido do tipo apos o patch.
 * Efeitos colaterais: nenhum.
 *
 * @param typeId identificador do tipo atualizado.
 * @param nome nome final persistido do tipo.
 * @param imagem imagem final serializada em data URI, quando houver.
 * @param inventoryId identificador do inventario ao qual o tipo pertence.
 */
public record UpdateTypeResponse(
        @JsonProperty("type_id")
        long typeId,
        String nome,
        String imagem,
        @JsonProperty("inv_id")
        UUID inventoryId
) {
}
