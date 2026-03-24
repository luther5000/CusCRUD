package br.com.cuscrudrest.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Configuracao da infraestrutura JDBC do projeto.
 * Cria DataSource e JdbcClient apenas quando as credenciais do banco estao presentes no ambiente.
 * Efeitos colaterais: registra beans de acesso a dados no contexto Spring.
 */
@Configuration(proxyBeanMethods = false)
@Conditional(DatabaseConfiguredCondition.class)
@EnableConfigurationProperties(CusCrudDatabaseProperties.class)
public class DatabaseConfig {

    /**
     * Cria o DataSource JDBC da aplicacao.
     * Estrategia: monta um DriverManagerDataSource simples a partir das propriedades de ambiente.
     * Efeitos colaterais: disponibiliza conexao JDBC para os componentes de persistencia.
     *
     * @param properties propriedades de conexao do banco da aplicacao.
     * @return DataSource configurado com URL, usuario e senha informados.
     */
    @Bean
    public DataSource dataSource(CusCrudDatabaseProperties properties) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(properties.url());
        dataSource.setUsername(properties.user());
        dataSource.setPassword(properties.password());
        return dataSource;
    }

    /**
     * Cria o facade JDBC usada pelos repositorios.
     * Estrategia: encapsula o DataSource em um JdbcClient para consultas nomeadas e atualizacoes SQL.
     * Efeitos colaterais: nenhum adicional alem do registro do bean no contexto.
     *
     * @param dataSource fonte de conexoes JDBC da aplicacao.
     * @return JdbcClient pronto para uso pelos repositorios.
     */
    @Bean
    public JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }
}
