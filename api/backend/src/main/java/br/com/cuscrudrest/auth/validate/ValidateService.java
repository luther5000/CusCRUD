package br.com.cuscrudrest.auth.validate;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import org.springframework.stereotype.Service;

/**
 * Servico de validacao do token JWT da aplicacao.
 * Converte o principal autenticado do Spring Security no payload do endpoint de validacao.
 * Efeitos colaterais: nenhum. Opera apenas sobre os dados ja autenticados da request atual.
 */
@Service
public class ValidateService {

    /**
     * Monta a resposta do endpoint de validacao a partir do principal autenticado.
     * Estrategia: reaproveita os dados de usuario e metadados do token resolvidos pelo filtro JWT.
     * Efeitos colaterais: nenhum.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @return usuario autenticado e metadados do token validado.
     */
    public ValidateResponse validate(AuthenticatedUserPrincipal authenticatedUser) {
        return new ValidateResponse(
                new ValidateUserResponse(
                        authenticatedUser.userId(),
                        authenticatedUser.name(),
                        authenticatedUser.login(),
                        authenticatedUser.createdAt()
                ),
                new ValidateTokenResponse(
                        authenticatedUser.expiresIn(),
                        authenticatedUser.issuedAt()
                )
        );
    }
}
