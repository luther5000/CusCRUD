package br.com.cuscrudrest.auth.security;

import br.com.cuscrudrest.auth.jwt.JwtService;
import br.com.cuscrudrest.auth.jwt.ValidatedJwtToken;
import br.com.cuscrudrest.auth.user.UserAccount;
import br.com.cuscrudrest.auth.user.UserRepository;
import br.com.cuscrudrest.common.error.UnauthenticatedException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT reutilizavel da aplicacao.
 * Extrai o header Authorization, valida o token, recarrega o usuario e popula o SecurityContext para rotas protegidas.
 * Efeitos colaterais: estabelece a autenticacao da request atual no SecurityContext.
 */
@Component
@Conditional(DatabaseConfiguredCondition.class)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Cria o filtro JWT da aplicacao.
     *
     * @param jwtService servico responsavel por extrair e validar o token.
     * @param userRepository repositorio JDBC dos usuarios.
     * @param authenticationEntryPoint entry point usado para responder 401 em formato padrao.
     */
    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = jwtService.extractBearerToken(authorizationHeader);
            ValidatedJwtToken validatedJwtToken = jwtService.validateToken(token);
            UserAccount userAccount = userRepository.findByUserId(validatedJwtToken.userId())
                    .orElseThrow(() -> new UnauthenticatedException(
                            "Token ausente, invalido ou expirado.",
                            "Authorization",
                            "jwt subject user not found"
                    ));

            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                    userAccount.userId(),
                    userAccount.name(),
                    userAccount.login(),
                    userAccount.createdAt(),
                    validatedJwtToken.issuedAt(),
                    validatedJwtToken.expiresAt(),
                    validatedJwtToken.expiresIn()
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (UnauthenticatedException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new SecurityAuthenticationException(
                            exception.getMessage(),
                            exception.getCampo(),
                            exception.getInfo()
                    )
            );
        }
    }
}
