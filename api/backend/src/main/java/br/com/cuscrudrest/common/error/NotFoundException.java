package br.com.cuscrudrest.common.error;

/**
 * Excecao de recurso inexistente para respostas HTTP 404.
 * Representa identificadores validos que nao correspondem a um recurso disponivel para a operacao.
 * Efeitos colaterais: nenhum.
 */
public class NotFoundException extends RuntimeException {

    private final String campo;
    private final String info;

    /**
     * Cria uma excecao de nao encontrado com mensagem e metadados do campo afetado.
     *
     * @param message descricao curta e acionavel do recurso nao encontrado.
     * @param campo nome do campo relacionado a busca do recurso.
     * @param info detalhe resumido do motivo do nao encontrado.
     */
    public NotFoundException(String message, String campo, String info) {
        super(message);
        this.campo = campo;
        this.info = info;
    }

    /**
     * @return campo associado ao recurso nao encontrado.
     */
    public String getCampo() {
        return campo;
    }

    /**
     * @return detalhe resumido do nao encontrado.
     */
    public String getInfo() {
        return info;
    }
}
