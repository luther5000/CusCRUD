package br.com.cuscrudrest.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryTest {

    private UserRepository userRepository;
    private JdbcClient jdbcClient;

    /**
     * Prepara um banco em memoria com o schema minimo da tabela `users`.
     * Entrada: nenhuma.
     * Esperado: DataSource H2, JdbcClient e UserRepository prontos para cada teste.
     */
    @BeforeEach
    void setUp() {
        DataSource dataSource = createDataSource();
        jdbcClient = JdbcClient.create(dataSource);
        jdbcClient.sql("DROP TABLE IF EXISTS users").update();
        jdbcClient.sql("""
                CREATE TABLE users (
                    user_id UUID DEFAULT random_uuid() PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    login VARCHAR(255) UNIQUE NOT NULL,
                    passwd TEXT NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """)
                .update();
        userRepository = new UserRepository(jdbcClient);
    }

    /**
     * Verifica que o repositorio insere um usuario e o devolve com os campos gerados pelo banco.
     * Entrada: nome, login e hash de senha validos.
     * Esperado: `user_id` e `created_at` preenchidos, com nome e login persistidos.
     */
    @Test
    void shouldInsertUserIntoUsersTable() {
        UserAccount insertedUser = userRepository.insertUser(
                "Joao Novo",
                "joao.novo@example.com",
                "$2a$10$abcdefghijklmnopqrstuv"
        );

        assertNotNull(insertedUser.userId());
        assertNotNull(insertedUser.createdAt());
        assertEquals("Joao Novo", insertedUser.name());
        assertEquals("joao.novo@example.com", insertedUser.login());
    }

    /**
     * Verifica que o repositorio identifica quando um login ja existe.
     * Entrada: login inserido previamente na tabela `users`.
     * Esperado: retorno true para o login existente.
     */
    @Test
    void shouldReportExistingLogin() {
        userRepository.insertUser("Joao Novo", "joao.novo@example.com", "$2a$10$abcdefghijklmnopqrstuv");

        assertTrue(userRepository.existsByLogin("joao.novo@example.com"));
    }

    /**
     * Verifica que o repositorio retorna false quando o login ainda nao existe.
     * Entrada: login nao persistido.
     * Esperado: retorno false.
     */
    @Test
    void shouldReportMissingLogin() {
        assertFalse(userRepository.existsByLogin("inexistente@example.com"));
    }

    /**
     * Cria o DataSource H2 usado pelos testes do repositorio.
     * Estrategia: usa modo de compatibilidade PostgreSQL e mantem o banco em memoria ativo durante a JVM.
     * Efeitos colaterais: nenhum fora da criacao do objeto.
     *
     * @return DataSource apontando para um banco H2 em memoria.
     */
    private DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:auth_register_deps;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
