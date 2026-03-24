package br.com.cuscrudrest.shared;

/**
 * Detalhes adicionais de erro retornados pela API.
 * Permite identificar o campo afetado e uma informacao curta para depuracao do cliente.
 * Efeitos colaterais: nenhum.
 *
 * @param field nome do campo relacionado ao erro, quando aplicavel.
 * @param info informacao curta adicional sobre a falha.
 */
public record ApiErrorDetails(String field, String info) {
}
