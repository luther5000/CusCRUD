package br.com.cuscrudrest.products;

/**
 * Constantes de validacao numerica compartilhadas pelo dominio de produtos.
 * Centraliza os limites aceitos para campos quantitativos expostos pela API.
 * Efeitos colaterais: nenhum.
 */
public final class ProductNumericConstraints {

    public static final long MAX_PRODUCT_AMOUNT = 999_999_999_999_999_999L;
    public static final String MAX_PRODUCT_AMOUNT_MESSAGE = "must be less than or equal to 999999999999999999";
    public static final String MIN_PRODUCT_AMOUNT_MESSAGE = "must be greater than or equal to 0";

    private ProductNumericConstraints() {
    }
}
