package br.com.cuscrudrest.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Controller de checagem operacional da aplicacao.
 * Exponde endpoints publicos para validar que o processo HTTP esta ativo e respondendo.
 * Efeitos colaterais: nenhum. Nao acessa banco de dados nem servicos externos.
 */
@RestController
public class HealthController {

    private static final ZoneId APPLICATION_TIME_ZONE = ZoneId.of("America/Recife");

    /**
     * GET /api/v1/health
     * Retorna status 200 com payload simples para checagem de vida do backend.
     * Estrategia: monta um DTO imutavel com status fixo "ok" e o instante atual na timezone da aplicacao.
     * Efeitos colaterais: nenhum. Nao acessa banco de dados nem modifica estado.
     *
     * @return DTO com o status operacional e o timestamp atual da resposta.
     */
    @GetMapping("/health")
    public HealthResponse getHealth() {
        return new HealthResponse("ok", OffsetDateTime.now(APPLICATION_TIME_ZONE));
    }
}
