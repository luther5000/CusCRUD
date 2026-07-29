package br.com.cuscrudrest.common.error;

/**
 * Excecao de autorizacao para respostas HTTP 403.
 * Representa operacoes autenticadas barradas por falta de permissao suficiente no recurso.
 * Efeitos colaterais: nenhum.
 */
public class ForbiddenException extends RuntimeException {

    private final String campo;
    private final String info;

    /**
     * Cria uma excecao de autorizacao com mensagem e metadados do campo afetado.
     *
     * @param message descricao curta e acionavel da falta de permissao.
     * @param campo nome do campo relacionado ao recurso protegido.
     * @param info detalhe resumido do motivo da negacao.
     */
    public ForbiddenException(String message, String campo, String info) {
        super(message);
        this.campo = campo;
        this.info = info;
    }

    /**
     * @return campo associado a falha de autorizacao.
     */
    public String getCampo() {
        return campo;
    }

    /**
     * @return detalhe resumido da falha de autorizacao.
     */
    public String getInfo() {
        return info;
    }
}
