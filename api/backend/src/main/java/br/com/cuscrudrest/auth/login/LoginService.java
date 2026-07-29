package br.com.cuscrudrest.auth.login;

import br.com.cuscrudrest.auth.jwt.IssuedJwtToken;
import br.com.cuscrudrest.auth.jwt.JwtService;
import br.com.cuscrudrest.auth.support.EmailAddressValidator;
import br.com.cuscrudrest.auth.support.PasswordHasher;
import br.com.cuscrudrest.auth.user.UserCredentials;
import br.com.cuscrudrest.auth.user.UserRepository;
import br.com.cuscrudrest.common.error.UnauthenticatedException;
import br.com.cuscrudrest.common.error.ValidationException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * Servico de autenticacao por login da aplicacao.
 * Coordena validacao do payload, consulta do usuario, verificacao Bcrypt e emissao do JWT.
 * Efeitos colaterais: emite um novo token em memoria para credenciais validas.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class LoginService {

    private final EmailAddressValidator emailAddressValidator;
    private final PasswordHasher passwordHasher;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    /**
     * Cria o servico de login.
     *
     * @param emailAddressValidator validador de formato de email.
     * @param passwordHasher componente responsavel pela comparacao da senha com o hash persistido.
     * @param userRepository repositorio JDBC dos usuarios.
     * @param jwtService servico de emissao de tokens JWT.
     */
    public LoginService(
            EmailAddressValidator emailAddressValidator,
            PasswordHasher passwordHasher,
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.emailAddressValidator = emailAddressValidator;
        this.passwordHasher = passwordHasher;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * Autentica um usuario e emite um novo token JWT.
     * Estrategia: valida o formato do email, carrega o usuario pelo login, compara a senha via Bcrypt e monta a resposta HTTP.
     * Efeitos colaterais: nenhum alem da emissao do token em memoria.
     *
     * @param request payload de login recebido pela camada HTTP.
     * @return token JWT e dados publicos do usuario autenticado.
     * @throws ValidationException quando o email informado nao possui formato valido.
     * @throws UnauthenticatedException quando o login nao existir ou a senha estiver incorreta.
     */
    public LoginResponse login(LoginRequest request) {
        validateEmail(request.login());

        UserCredentials userCredentials = userRepository.findCredentialsByLogin(request.login())
                .orElseThrow(() -> invalidLogin());

        if (!passwordHasher.matches(request.passwd(), userCredentials.encodedPassword())) {
            throw new UnauthenticatedException(
                    "Credenciais invalidas.",
                    "passwd",
                    "incorrect password"
            );
        }

        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userCredentials.userId());

        return new LoginResponse(
                issuedJwtToken.token(),
                issuedJwtToken.expiresIn(),
                new LoginUserResponse(
                        userCredentials.userId(),
                        userCredentials.name(),
                        userCredentials.login(),
                        userCredentials.createdAt()
                )
        );
    }

    /**
     * Valida o formato do email de login.
     *
     * @param login email informado no request.
     * @throws ValidationException quando o email nao atende ao formato esperado.
     */
    private void validateEmail(String login) {
        if (!emailAddressValidator.isValid(login)) {
            throw new ValidationException("Login invalido.", "login", "must be a valid email");
        }
    }

    /**
     * Constrói a excecao padrao para login inexistente.
     *
     * @return excecao HTTP 401 consistente com o contrato da API.
     */
    private UnauthenticatedException invalidLogin() {
        return new UnauthenticatedException(
                "Credenciais invalidas.",
                "login",
                "login not found"
        );
    }
}
