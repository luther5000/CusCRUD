package br.com.cuscrudrest.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Fabrica o contexto de logging de cada request HTTP.
 * Resolve o IP efetivo do cliente e gera um identificador de request que incorpora esse IP.
 * Efeitos colaterais: nenhum.
 */
@Component
public class RequestLogContextFactory {

    private static final String UNKNOWN_IP = "unknown";

    /**
     * Cria o contexto de logging para a request informada.
     *
     * @param request request HTTP atual.
     * @return contexto com `request_id` e `client_ip`.
     */
    public RequestLogContext create(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        return new RequestLogContext(buildRequestId(clientIp), clientIp);
    }

    /**
     * Resolve o IP efetivo do cliente.
     * Estrategia: usa o primeiro valor de `X-Forwarded-For` quando presente; caso contrario, usa `remoteAddr`.
     *
     * @param request request HTTP atual.
     * @return IP efetivo do cliente ou `unknown` quando indisponivel.
     */
    String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String firstForwardedIp = forwardedFor.split(",")[0].trim();
            if (!firstForwardedIp.isBlank()) {
                return firstForwardedIp;
            }
        }

        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return UNKNOWN_IP;
        }

        return remoteAddress;
    }

    /**
     * Monta o identificador da request a partir do IP do cliente e um sufixo aleatorio curto.
     *
     * @param clientIp IP efetivo do cliente.
     * @return identificador rastreavel da request contendo o IP sanitizado.
     */
    String buildRequestId(String clientIp) {
        String sanitizedIp = clientIp.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        return sanitizedIp + "-" + randomSuffix;
    }
}
