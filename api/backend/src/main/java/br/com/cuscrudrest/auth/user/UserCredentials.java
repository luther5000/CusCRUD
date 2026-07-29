package br.com.cuscrudrest.auth.user;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representa o usuario com os dados necessarios para autenticacao.
 * Expõe o hash persistido da senha apenas para a camada interna de login.
 * Efeitos colaterais: nenhum.
 *
 * @param userId identificador unico do usuario no banco.
 * @param name nome do usuario.
 * @param login email unico do usuario.
 * @param encodedPassword hash persistido da senha.
 * @param createdAt instante de criacao do cadastro.
 */
public record UserCredentials(
        UUID userId,
        String name,
        String login,
        String encodedPassword,
        OffsetDateTime createdAt
) {
}
