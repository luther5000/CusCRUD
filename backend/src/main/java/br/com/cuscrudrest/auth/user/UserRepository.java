package br.com.cuscrudrest.auth.user;

import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JDBC dos usuarios da aplicacao.
 * Executa leituras e escritas na tabela `users` usando SQL explicito.
 * Efeitos colaterais: cria e consulta registros persistidos no banco.
 */
@Repository
@Conditional(DatabaseConfiguredCondition.class)
public class UserRepository {

    private static final RowMapper<UserAccount> USER_ACCOUNT_ROW_MAPPER = UserRepository::mapUserAccount;
    private static final RowMapper<UserCredentials> USER_CREDENTIALS_ROW_MAPPER = UserRepository::mapUserCredentials;

    private final JdbcClient jdbcClient;

    /**
     * Cria o repositorio de usuarios.
     *
     * @param jdbcClient facade JDBC configurada para o banco da aplicacao.
     */
    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Verifica se ja existe um usuario com o login informado.
     * Estrategia: executa uma contagem simples por `login`.
     * Efeitos colaterais: nenhum alem da leitura da tabela `users`.
     *
     * @param login email unico a ser consultado.
     * @return true quando existe ao menos um usuario com o login informado; false caso contrario.
     */
    public boolean existsByLogin(String login) {
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM users WHERE login = :login")
                .param("login", login)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    /**
     * Insere um novo usuario na tabela `users`.
     * Estrategia: executa o insert com o hash ja calculado e relê o registro pelo login para devolver os dados persistidos.
     * Efeitos colaterais: cria um novo registro na tabela `users`.
     *
     * @param name nome do usuario a ser criado.
     * @param login email unico do usuario a ser criado.
     * @param encodedPassword hash da senha gerado previamente pela camada de autenticacao.
     * @return usuario persistido com `user_id` e `created_at` preenchidos pelo banco.
     * @throws IllegalStateException quando o insert e concluido, mas o usuario nao pode ser relido da base.
     */
    public UserAccount insertUser(String name, String login, String encodedPassword) {
        jdbcClient.sql("""
                INSERT INTO users (name, login, passwd)
                VALUES (:name, :login, :passwd)
                """)
                .param("name", name)
                .param("login", login)
                .param("passwd", encodedPassword)
                .update();

        return findByLogin(login)
                .orElseThrow(() -> new IllegalStateException("User was inserted but could not be reloaded"));
    }

    /**
     * Busca um usuario pelo login.
     * Estrategia: usa query por `login` e converte a linha retornada para o record `UserAccount`.
     * Efeitos colaterais: nenhum alem da leitura da tabela `users`.
     *
     * @param login email unico do usuario a ser localizado.
     * @return usuario encontrado, ou vazio quando nao existir registro para o login informado.
     */
    public Optional<UserAccount> findByLogin(String login) {
        return jdbcClient.sql("""
                SELECT user_id, name, login, created_at
                FROM users
                WHERE login = :login
                """)
                .param("login", login)
                .query(USER_ACCOUNT_ROW_MAPPER)
                .optional();
    }

    /**
     * Busca um usuario pelo login incluindo o hash persistido da senha.
     * Estrategia: consulta a tabela `users` por `login` para atender o fluxo de autenticacao.
     * Efeitos colaterais: nenhum alem da leitura da tabela `users`.
     *
     * @param login email unico do usuario a ser autenticado.
     * @return usuario com credenciais persistidas, ou vazio quando nao existir registro para o login informado.
     */
    public Optional<UserCredentials> findCredentialsByLogin(String login) {
        return jdbcClient.sql("""
                SELECT user_id, name, login, passwd, created_at
                FROM users
                WHERE login = :login
                """)
                .param("login", login)
                .query(USER_CREDENTIALS_ROW_MAPPER)
                .optional();
    }

    /**
     * Converte a linha JDBC em um `UserAccount`.
     *
     * @param resultSet linha atual retornada pela consulta JDBC.
     * @param rowNum indice da linha no cursor da consulta.
     * @return representacao imutavel do usuario persistido.
     * @throws SQLException quando a leitura das colunas falha.
     */
    private static UserAccount mapUserAccount(ResultSet resultSet, int rowNum) throws SQLException {
        UUID userId = resultSet.getObject("user_id", UUID.class);
        String name = resultSet.getString("name");
        String login = resultSet.getString("login");
        OffsetDateTime createdAt = resultSet.getObject("created_at", OffsetDateTime.class);
        return new UserAccount(userId, name, login, createdAt);
    }

    /**
     * Converte a linha JDBC em um `UserCredentials`.
     *
     * @param resultSet linha atual retornada pela consulta JDBC.
     * @param rowNum indice da linha no cursor da consulta.
     * @return representacao imutavel do usuario com hash persistido.
     * @throws SQLException quando a leitura das colunas falha.
     */
    private static UserCredentials mapUserCredentials(ResultSet resultSet, int rowNum) throws SQLException {
        UUID userId = resultSet.getObject("user_id", UUID.class);
        String name = resultSet.getString("name");
        String login = resultSet.getString("login");
        String encodedPassword = resultSet.getString("passwd");
        OffsetDateTime createdAt = resultSet.getObject("created_at", OffsetDateTime.class);
        return new UserCredentials(userId, name, login, encodedPassword, createdAt);
    }
}
