package br.com.cuscrudrest.products.update;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Payload de entrada do endpoint de atualizacao parcial de produtos.
 * Representa os campos opcionais aceitos pela API para aplicar patch em um produto existente.
 * Efeitos colaterais: nenhum.
 *
 * @param typeId novo identificador de tipo, quando informado.
 * @param marca nova marca ou fabricante, quando informada.
 * @param dataValidade nova data de validade, quando informada.
 * @param unidade nova unidade base, quando informada.
 * @param unidadeMedida novo texto da unidade de medida, quando informado.
 * @param quantidade nova quantidade do produto, quando informada.
 */
public record UpdateProductRequest(
        @JsonProperty("type_id")
        JsonNode typeId,
        JsonNode marca,
        JsonNode dataValidade,
        JsonNode unidade,
        JsonNode unidadeMedida,
        JsonNode quantidade
) {

    /**
     * Indica se o payload nao trouxe nenhum campo reconhecido para o patch.
     * Estrategia: verifica simultaneamente se todos os atributos opcionais ficaram ausentes.
     * Efeitos colaterais: nenhum.
     *
     * @return `true` quando nao ha nenhum campo a atualizar; `false` caso contrario.
     */
    public boolean isEmpty() {
        return typeId == null
                && marca == null
                && dataValidade == null
                && unidade == null
                && unidadeMedida == null
                && quantidade == null;
    }
}
