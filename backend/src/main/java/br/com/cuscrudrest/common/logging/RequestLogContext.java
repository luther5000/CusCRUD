package br.com.cuscrudrest.common.logging;

/**
 * Contexto minimo de logging por request.
 * Agrupa o identificador rastreavel da requisicao e o IP efetivo do cliente para uso em MDC.
 * Efeitos colaterais: nenhum.
 *
 * @param requestId identificador da request contendo o IP do cliente e um sufixo aleatorio.
 * @param clientIp endereco IP efetivo do cliente.
 */
public record RequestLogContext(String requestId, String clientIp) {
}
