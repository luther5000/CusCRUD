package br.com.cuscrudrest.auth;

import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.auth.login.LoginRequest;
import br.com.cuscrudrest.auth.login.LoginResponse;
import br.com.cuscrudrest.auth.login.LoginService;
import br.com.cuscrudrest.auth.register.RegisterRequest;
import br.com.cuscrudrest.auth.register.RegisterResponse;
import br.com.cuscrudrest.auth.register.RegisterService;
import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.auth.validate.ValidateResponse;
import br.com.cuscrudrest.auth.validate.ValidateService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller HTTP dos endpoints de autenticacao.
 * Exponde operacoes publicas de cadastro e, futuramente, login e validacao de token.
 * Efeitos colaterais: cria usuarios persistidos ao processar cadastros validos.
 */
@RestController
@Conditional(DatabaseConfiguredCondition.class)
public class AuthController {

    private final LoginService loginService;
    private final RegisterService registerService;
    private final ValidateService validateService;

    /**
     * Cria o controller de autenticacao.
     *
     * @param loginService servico de negocio responsavel pelo login e emissao de token.
     * @param registerService servico de negocio responsavel pelo cadastro de usuario.
     * @param validateService servico de negocio responsavel pela validacao do JWT.
     */
    public AuthController(LoginService loginService, RegisterService registerService, ValidateService validateService) {
        this.loginService = loginService;
        this.registerService = registerService;
        this.validateService = validateService;
    }

    /**
     * POST /api/v1/auth/login
     * Autentica o usuario a partir de login e senha validos e retorna um token JWT com TTL fixo.
     * Estrategia: valida o corpo via Bean Validation e delega a verificacao de credenciais ao servico de login.
     * Efeitos colaterais: nenhum alem da emissao do token em memoria.
     *
     * @param request payload HTTP com as credenciais do usuario.
     * @return token JWT e dados publicos do usuario autenticado.
     */
    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    /**
     * GET /api/v1/auth/validate
     * Retorna os dados do usuario autenticado a partir do principal carregado pelo filtro JWT.
     * Estrategia: consome o principal da request atual ja autenticada pelo Spring Security.
     * Efeitos colaterais: nenhum.
     *
     * @param authenticatedUser principal autenticado resolvido pelo Spring Security.
     * @return dados publicos do usuario autenticado e metadados do token.
     */
    @GetMapping("/auth/validate")
    public ValidateResponse validate(@AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser) {
        return validateService.validate(authenticatedUser);
    }

    /**
     * POST /api/v1/auth/register
     * Cria um novo usuario com nome, email unico e senha fornecidos no payload.
     * Estrategia: valida o corpo via Bean Validation e delega as regras de negocio ao servico de autenticacao.
     * Efeitos colaterais: persiste um novo registro na tabela `users` quando o cadastro e valido.
     *
     * @param request payload HTTP com os dados do usuario a ser criado.
     * @return dados publicos do usuario criado.
     */
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return registerService.register(request);
    }
}
