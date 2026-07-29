package br.com.cuscrudrest.common.error;

/**
 * Envelope padrao de erro retornado pela API.
 * Mantem compatibilidade com o contrato definido no documento de arquitetura.
 * Efeitos colaterais: nenhum.
 *
 * @param error objeto de erro principal da resposta HTTP.
 */
public record ApiErrorResponse(ApiErrorBody error) {
}
