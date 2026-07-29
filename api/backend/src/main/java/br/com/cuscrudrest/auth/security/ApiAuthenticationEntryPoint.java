package br.com.cuscrudrest.auth.security;

import br.com.cuscrudrest.common.error.ApiErrorBody;
import br.com.cuscrudrest.common.error.ApiErrorDetails;
import br.com.cuscrudrest.common.error.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Entry point do Spring Security para respostas 401 no formato padrao da API.
 * Centraliza a serializacao do erro de autenticacao tanto para token ausente quanto para token invalido.
 * Efeitos colaterais: escreve a resposta HTTP diretamente no servlet response.
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Cria o entry point de autenticacao da API.
     *
     * @param objectMapper serializador JSON da aplicacao.
     */
    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        String message = "Token nao enviado ou invalido.";
        String campo = "Authorization";
        String info = "missing bearer token";

        if (authException instanceof SecurityAuthenticationException securityAuthenticationException) {
            message = securityAuthenticationException.getMessage();
            campo = securityAuthenticationException.getCampo();
            info = securityAuthenticationException.getInfo();
        }

        ApiErrorResponse body = new ApiErrorResponse(
                new ApiErrorBody(
                        "UNAUTHENTICATED",
                        message,
                        new ApiErrorDetails(campo, info)
                )
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
