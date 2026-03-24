package br.com.cuscrudrest.auth;

import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
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

    private final AuthService authService;

    /**
     * Cria o controller de autenticacao.
     *
     * @param authService servico de negocio que coordena cadastro e autenticacao.
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
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
        return authService.register(request);
    }
}
