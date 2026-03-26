package br.com.cuscrudrest.products.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Payload de entrada do endpoint de criacao de produtos.
 * Representa os dados aceitos pela API para criar um novo produto em um inventario.
 * Efeitos colaterais: nenhum.
 *
 * @param typeId identificador do tipo ao qual o produto pertence.
 * @param marca marca ou fabricante do produto, quando houver.
 * @param dataValidade data de validade com timezone, quando houver.
 * @param unidade unidade base do produto, quando houver.
 * @param unidadeMedida texto da unidade de medida, quando houver.
 * @param quantidade quantidade inicial do produto, quando houver.
 */
public record CreateProductRequest(
        @JsonProperty("type_id")
        @NotNull(message = "must not be null")
        Long typeId,
        @Size(max = 255, message = "must have at most 255 characters")
        String marca,
        OffsetDateTime dataValidade,
        @PositiveOrZero(message = "must be greater than or equal to 0")
        Long unidade,
        @Size(max = 255, message = "must have at most 255 characters")
        String unidadeMedida,
        @PositiveOrZero(message = "must be greater than or equal to 0")
        Long quantidade
) {
}
