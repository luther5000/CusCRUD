package br.com.cuscrudrest.health;

import java.time.OffsetDateTime;

/**
 * DTO de resposta da checagem operacional do backend.
 * Transporta um status textual e o instante da resposta.
 * Efeitos colaterais: nenhum.
 *
 * @param status identificador textual do estado operacional atual da aplicacao.
 * @param time instante em que a resposta foi gerada, com offset de timezone.
 */
public record HealthResponse(String status, OffsetDateTime time) {
}
