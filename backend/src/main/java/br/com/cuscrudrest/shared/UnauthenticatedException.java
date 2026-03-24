package br.com.cuscrudrest.shared;

/**
 * Excecao de autenticacao para respostas HTTP 401.
 * Representa ausencia ou invalidade de credenciais de acesso da API.
 * Efeitos colaterais: nenhum.
 */
public class UnauthenticatedException extends RuntimeException {

    private final String campo;
    private final String info;

    /**
     * Cria uma excecao de autenticacao com mensagem e metadados opcionais.
     *
     * @param message descricao curta e acionavel do problema de autenticacao.
     * @param campo nome do campo relacionado ao erro, quando aplicavel.
     * @param info detalhe resumido adicional da falha.
     */
    public UnauthenticatedException(String message, String campo, String info) {
        super(message);
        this.campo = campo;
        this.info = info;
    }

    /**
     * @return campo associado ao erro de autenticacao, quando houver.
     */
    public String getCampo() {
        return campo;
    }

    /**
     * @return detalhe resumido da falha de autenticacao.
     */
    public String getInfo() {
        return info;
    }
}
