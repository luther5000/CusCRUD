package br.com.cuscrudrest.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representa um usuario persistido no sistema.
 * Transporta os dados publicos relevantes do cadastro, sem expor o hash da senha.
 * Efeitos colaterais: nenhum.
 *
 * @param userId identificador unico do usuario no banco.
 * @param name nome do usuario.
 * @param login email unico do usuario.
 * @param createdAt instante de criacao do cadastro.
 */
public record UserAccount(UUID userId, String name, String login, OffsetDateTime createdAt) {
}
