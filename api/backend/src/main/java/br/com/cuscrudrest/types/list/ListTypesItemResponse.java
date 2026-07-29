package br.com.cuscrudrest.types.list;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Item individual da resposta de listagem de tipos.
 * Expõe o identificador, nome e presenca de imagem do tipo.
 * Efeitos colaterais: nenhum.
 *
 * @param typeId identificador do tipo.
 * @param nome nome do tipo.
 * @param hasImage indica se o tipo possui imagem.
 */
public record ListTypesItemResponse(
        @JsonProperty("type_id")
        long typeId,
        String nome,
        @JsonProperty("has_image")
        boolean hasImage
) {
}
