package br.com.cuscrudrest.auth.security;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Principal autenticado da aplicacao carregado a partir do JWT.
 * Expõe os dados publicos do usuario e os metadados temporais do token para consumo pelos endpoints protegidos.
 * Efeitos colaterais: nenhum.
 *
 * @param userId identificador unico do usuario autenticado.
 * @param name nome persistido do usuario.
 * @param login email persistido do usuario.
 * @param createdAt instante de criacao do cadastro.
 * @param issuedAt instante de emissao do token.
 * @param expiresAt instante de expiracao do token.
 * @param expiresIn TTL fixo do token em segundos.
 */
public record AuthenticatedUserPrincipal(
        UUID userId,
        String name,
        String login,
        OffsetDateTime createdAt,
        OffsetDateTime issuedAt,
        OffsetDateTime expiresAt,
        long expiresIn
) implements Principal {

    @Override
    public String getName() {
        return login;
    }
}
