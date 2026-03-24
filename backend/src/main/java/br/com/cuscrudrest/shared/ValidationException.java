package br.com.cuscrudrest.shared;

/**
 * Excecao de validacao de negocio para respostas HTTP 400.
 * Representa entradas sintaticamente presentes, mas semanticamente invalidas.
 * Efeitos colaterais: nenhum.
 */
public class ValidationException extends RuntimeException {

    private final String campo;
    private final String info;

    /**
     * Cria uma excecao de validacao com mensagem e metadados do campo afetado.
     *
     * @param message descricao curta e acionavel da falha de validacao.
     * @param campo nome do campo relacionado a falha.
     * @param info detalhe resumido do motivo da invalidacao.
     */
    public ValidationException(String message, String campo, String info) {
        super(message);
        this.campo = campo;
        this.info = info;
    }

    /**
     * @return campo associado a falha de validacao.
     */
    public String getCampo() {
        return campo;
    }

    /**
     * @return detalhe resumido da falha.
     */
    public String getInfo() {
        return info;
    }
}
