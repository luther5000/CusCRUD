package br.com.cuscrudrest.auth;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Component;

/**
 * Validador de enderecos de email usados pela camada de autenticacao.
 * Centraliza a regra de formato para cadastro, login e compartilhamento de acesso.
 * Efeitos colaterais: nenhum.
 */
@Component
public class EmailAddressValidator {

    private final EmailValidator delegate = EmailValidator.getInstance();

    /**
     * Verifica se um email atende ao formato esperado pela aplicacao.
     * Estrategia: delega a validacao sintatica para o Apache Commons Validator.
     * Efeitos colaterais: nenhum.
     *
     * @param email endereco de email informado pela camada HTTP ou de servico.
     * @return true quando o email possui formato valido; false caso contrario.
     */
    public boolean isValid(String email) {
        return delegate.isValid(email);
    }
}
