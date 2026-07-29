package br.com.cuscrudrest.auth.validate;

/**
 * Payload de saida do endpoint de validacao de token.
 * Contem os dados publicos do usuario autenticado e os metadados temporais do JWT.
 * Efeitos colaterais: nenhum.
 *
 * @param user dados publicos do usuario autenticado.
 * @param token metadados do token validado.
 */
public record ValidateResponse(ValidateUserResponse user, ValidateTokenResponse token) {
}
