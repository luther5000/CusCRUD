package br.com.cuscrudrest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de conexao com o banco do projeto.
 * Agrupa URL, usuario e senha vindos do ambiente para configuracao do DataSource.
 * Efeitos colaterais: nenhum.
 *
 * @param url URL JDBC do banco da aplicacao.
 * @param user usuario da aplicacao no banco.
 * @param password senha do usuario da aplicacao no banco.
 */
@ConfigurationProperties(prefix = "cuscrud.database")
public record CusCrudDatabaseProperties(String url, String user, String password) {
}
