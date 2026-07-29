package br.com.cuscrudrest.auth.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Excecao de autenticacao adaptada para o fluxo do Spring Security.
 * Preserva os metadados de erro do contrato HTTP ao atravessar o filtro de seguranca.
 * Efeitos colaterais: nenhum.
 */
public class SecurityAuthenticationException extends AuthenticationException {

    private final String campo;
    private final String info;

    /**
     * Cria a excecao de autenticacao do modulo de seguranca.
     *
     * @param message descricao curta e acionavel do problema de autenticacao.
     * @param campo campo relacionado ao erro, quando aplicavel.
     * @param info detalhe resumido adicional da falha.
     */
    public SecurityAuthenticationException(String message, String campo, String info) {
        super(message);
        this.campo = campo;
        this.info = info;
    }

    /**
     * @return campo associado ao erro de autenticacao.
     */
    public String getCampo() {
        return campo;
    }

    /**
     * @return detalhe resumido adicional da falha.
     */
    public String getInfo() {
        return info;
    }
}
