package br.com.cuscrudrest.auth.validate;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Metadados do token validados pela API.
 * Expõe o TTL fixo restante por contrato e o instante de emissao do JWT.
 * Efeitos colaterais: nenhum.
 *
 * @param expiresIn tempo de vida fixo do token em segundos.
 * @param issuedAt instante de emissao presente na claim `iat`.
 */
public record ValidateTokenResponse(
        @JsonProperty("expires_in")
        long expiresIn,
        @JsonProperty("issued_at")
        OffsetDateTime issuedAt
) {
}
