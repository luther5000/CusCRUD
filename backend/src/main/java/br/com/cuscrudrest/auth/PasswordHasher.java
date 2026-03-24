package br.com.cuscrudrest.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador de hash de senha da aplicacao.
 * Encapsula o PasswordEncoder para manter a regra de senha concentrada na camada de autenticacao.
 * Efeitos colaterais: nenhum.
 */
@Component
public class PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    /**
     * Cria o componente de hash de senha.
     *
     * @param passwordEncoder encoder configurado no contexto Spring para tratar `passwd`.
     */
    public PasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Gera o hash persistivel de uma senha em texto puro.
     * Estrategia: delega ao PasswordEncoder configurado para a aplicacao.
     * Efeitos colaterais: nenhum.
     *
     * @param rawPassword senha em texto puro recebida do fluxo de autenticacao.
     * @return representacao hasheada adequada para armazenamento.
     */
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verifica se uma senha em texto puro corresponde ao hash persistido.
     * Estrategia: delega ao PasswordEncoder configurado para comparar os valores.
     * Efeitos colaterais: nenhum.
     *
     * @param rawPassword senha informada no momento da autenticacao.
     * @param encodedPassword hash previamente persistido para o usuario.
     * @return true quando a senha corresponde ao hash; false caso contrario.
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
