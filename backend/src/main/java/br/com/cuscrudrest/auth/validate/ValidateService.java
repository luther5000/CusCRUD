package br.com.cuscrudrest.auth.validate;

import br.com.cuscrudrest.auth.jwt.JwtService;
import br.com.cuscrudrest.auth.jwt.ValidatedJwtToken;
import br.com.cuscrudrest.auth.user.UserAccount;
import br.com.cuscrudrest.auth.user.UserRepository;
import br.com.cuscrudrest.common.error.UnauthenticatedException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * Servico de validacao do token JWT da aplicacao.
 * Coordena a extração do header Authorization, a validacao do token e a carga do usuario autenticado.
 * Efeitos colaterais: nenhum. Opera apenas com leitura de banco e dados do token recebido.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class ValidateService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Cria o servico de validacao de token.
     *
     * @param jwtService servico responsavel por extrair e validar o JWT.
     * @param userRepository repositorio JDBC dos usuarios.
     */
    public ValidateService(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Valida o token Bearer recebido no header Authorization e retorna o usuario autenticado.
     * Estrategia: extrai o token do header, valida assinatura/claims e recarrega o usuario a partir da claim `sub`.
     * Efeitos colaterais: nenhum alem da leitura da tabela `users`.
     *
     * @param authorizationHeader valor bruto do header `Authorization`.
     * @return usuario autenticado e metadados do token validado.
     * @throws UnauthenticatedException quando o header ou o token forem invalidos, expirados ou referenciarem usuario inexistente.
     */
    public ValidateResponse validate(String authorizationHeader) {
        String token = jwtService.extractBearerToken(authorizationHeader);
        ValidatedJwtToken validatedJwtToken = jwtService.validateToken(token);

        UserAccount userAccount = userRepository.findByUserId(validatedJwtToken.userId())
                .orElseThrow(() -> new UnauthenticatedException(
                        "Token ausente, invalido ou expirado.",
                        "Authorization",
                        "jwt subject user not found"
                ));

        return new ValidateResponse(
                new ValidateUserResponse(
                        userAccount.userId(),
                        userAccount.name(),
                        userAccount.login(),
                        userAccount.createdAt()
                ),
                new ValidateTokenResponse(
                        validatedJwtToken.expiresIn(),
                        validatedJwtToken.issuedAt()
                )
        );
    }
}
