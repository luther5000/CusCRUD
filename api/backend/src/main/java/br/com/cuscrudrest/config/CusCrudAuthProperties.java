package br.com.cuscrudrest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de autenticacao da aplicacao.
 * Agrupa o segredo compartilhado do JWT e o tempo de vida fixo do token.
 * Efeitos colaterais: nenhum.
 *
 * @param jwtSecret segredo usado para assinatura HS256 dos tokens JWT.
 * @param jwtTtlSeconds tempo de vida fixo do token em segundos.
 */
@ConfigurationProperties(prefix = "cuscrud.auth")
public record CusCrudAuthProperties(String jwtSecret, long jwtTtlSeconds) {
}
