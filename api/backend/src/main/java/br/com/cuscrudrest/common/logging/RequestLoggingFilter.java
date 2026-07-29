package br.com.cuscrudrest.common.logging;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de access log da aplicacao.
 * Anexa `request_id` e `client_ip` ao MDC e registra cada request no nivel apropriado conforme a politica da especificacao.
 * Efeitos colaterais: escreve logs de request ao final do processamento HTTP.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final RequestLogContextFactory requestLogContextFactory;

    /**
     * Cria o filtro de logging de requests.
     *
     * @param requestLogContextFactory fabrica do contexto de logging por request.
     */
    public RequestLoggingFilter(ObjectProvider<RequestLogContextFactory> requestLogContextFactoryProvider) {
        this.requestLogContextFactory = requestLogContextFactoryProvider.getIfAvailable(RequestLogContextFactory::new);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RequestLogContext requestLogContext = requestLogContextFactory.create(request);
        long startedAtNanos = System.nanoTime();

        MDC.put("request_id", requestLogContext.requestId());
        MDC.put("client_ip", requestLogContext.clientIp());

        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                logRequest(request, response, startedAtNanos);
            } finally {
                MDC.remove("request_id");
                MDC.remove("client_ip");
            }
        }
    }

    /**
     * Decide o nivel do log e registra a request atual.
     *
     * @param request request HTTP processada.
     * @param response response HTTP resultante.
     * @param startedAtNanos instante de inicio do processamento em nanos.
     */
    private void logRequest(HttpServletRequest request, HttpServletResponse response, long startedAtNanos) {
        int status = response.getStatus();
        long durationMillis = (System.nanoTime() - startedAtNanos) / 1_000_000;
        String path = buildRequestPath(request);
        String userId = resolveAuthenticatedUserId();

        if (status >= 500) {
            log.error("http_request method={} path={} status={} duration_ms={} user_id={}",
                    request.getMethod(), path, status, durationMillis, userId);
            return;
        }

        if (status >= 400) {
            log.warn("http_request method={} path={} status={} duration_ms={} user_id={}",
                    request.getMethod(), path, status, durationMillis, userId);
            return;
        }

        if (isAuthenticationRequest(request) || isMutationRequest(request)) {
            log.warn("http_request method={} path={} status={} duration_ms={} user_id={}",
                    request.getMethod(), path, status, durationMillis, userId);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("http_request method={} path={} status={} duration_ms={} user_id={}",
                    request.getMethod(), path, status, durationMillis, userId);
        }
    }

    /**
     * Monta o path logado com query string quando presente.
     *
     * @param request request HTTP atual.
     * @return path completo da request.
     */
    private String buildRequestPath(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + queryString;
    }

    /**
     * Identifica requests do modulo de autenticacao.
     *
     * @param request request HTTP atual.
     * @return true quando o path pertence a `/auth`.
     */
    private boolean isAuthenticationRequest(HttpServletRequest request) {
        return request.getServletPath().startsWith("/auth/");
    }

    /**
     * Identifica requests de alteracao de dados.
     *
     * @param request request HTTP atual.
     * @return true quando o metodo HTTP altera estado.
     */
    private boolean isMutationRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }

    /**
     * Resolve o `user_id` do principal autenticado quando disponivel.
     *
     * @return identificador do usuario autenticado ou `anonymous`.
     */
    private String resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return "anonymous";
        }
        return principal.userId().toString();
    }
}
