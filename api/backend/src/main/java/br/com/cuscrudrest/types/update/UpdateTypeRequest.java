package br.com.cuscrudrest.types.update;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Payload de entrada do endpoint de atualizacao de tipos.
 * Preserva a distincao entre campo ausente e campo enviado com `null`, necessaria para o comportamento de patch.
 * Efeitos colaterais: nenhum.
 *
 * @param nome novo nome do tipo, quando enviado.
 * @param imagem nova imagem do tipo, `null` para remocao ou ausente para manter o valor atual.
 */
public record UpdateTypeRequest(
        JsonNode nome,
        JsonNode imagem
) {

    /**
     * @return `true` quando o patch nao contem nenhum campo reconhecido.
     */
    public boolean isEmpty() {
        return nome == null && imagem == null;
    }
}
