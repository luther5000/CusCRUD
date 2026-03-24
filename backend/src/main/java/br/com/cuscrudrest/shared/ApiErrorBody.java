package br.com.cuscrudrest.shared;

/**
 * Corpo interno do objeto `error` retornado pela API.
 * Encapsula codigo, mensagem e detalhes opcionais conforme a especificacao do projeto.
 * Efeitos colaterais: nenhum.
 *
 * @param code identificador interno estavel do tipo de erro.
 * @param message descricao curta e acionavel para o cliente.
 * @param details metadados opcionais associados ao erro.
 */
public record ApiErrorBody(String code, String message, ApiErrorDetails details) {
}
