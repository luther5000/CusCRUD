package br.com.cuscrudrest.shared;

/**
 * Excecao de conflito de negocio para respostas HTTP 409.
 * Representa estados validos do sistema que impedem a operacao solicitada.
 * Efeitos colaterais: nenhum.
 */
public class ConflictException extends RuntimeException {

    private final String field;
    private final String info;

    /**
     * Cria uma excecao de conflito com mensagem e metadados do campo afetado.
     *
     * @param message descricao curta e acionavel do conflito.
     * @param field nome do campo relacionado ao conflito.
     * @param info detalhe resumido do motivo do conflito.
     */
    public ConflictException(String message, String field, String info) {
        super(message);
        this.field = field;
        this.info = info;
    }

    /**
     * @return campo associado ao conflito.
     */
    public String getField() {
        return field;
    }

    /**
     * @return detalhe resumido do conflito.
     */
    public String getInfo() {
        return info;
    }
}
