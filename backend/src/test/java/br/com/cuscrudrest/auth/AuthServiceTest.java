package br.com.cuscrudrest.auth;

import br.com.cuscrudrest.shared.ConflictException;
import br.com.cuscrudrest.shared.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private JdbcClient jdbcClient;
    private AuthService authService;

    /**
     * Prepara o schema minimo e as dependencias reais do servico de cadastro.
     * Entrada: nenhuma.
     * Esperado: servico pronto para validar email, hashear senha e persistir usuarios.
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

        UserRepository userRepository = new UserRepository(jdbcClient);
        authService = new AuthService(
                new EmailAddressValidator(),
                new PasswordHasher(new BCryptPasswordEncoder()),
                userRepository
        );
    }

    /**
     * Verifica que o servico cadastra um usuario valido e retorna os dados publicos persistidos.
     * Entrada: request com nome, email unico e senha valida.
     * Esperado: resposta com identificador, nome, login e timestamp de criacao.
     */
    @Test
    void shouldRegisterUserWhenRequestIsValid() {
        RegisterResponse response = authService.register(
                new RegisterRequest("Joao Novo", "joao.novo@example.com", "senhaforte456")
        );

        assertNotNull(response.userId());
        assertEquals("Joao Novo", response.name());
        assertEquals("joao.novo@example.com", response.login());
        assertNotNull(response.createdAt());
    }

    /**
     * Verifica que o servico rejeita email fora do formato esperado.
     * Entrada: request com `login` malformado.
     * Esperado: `ValidationException` associada ao campo `login`.
     */
    @Test
    void shouldRejectMalformedEmail() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> authService.register(new RegisterRequest("Joao Novo", "joao.novoexample.com", "senhaforte456"))
        );

        assertEquals("login", exception.getCampo());
    }

    /**
     * Verifica que o servico rejeita login ja cadastrado.
     * Entrada: request com email previamente persistido.
     * Esperado: `ConflictException` associada ao campo `login`.
     */
    @Test
    void shouldRejectDuplicateLogin() {
        authService.register(new RegisterRequest("Joao Novo", "joao.novo@example.com", "senhaforte456"));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> authService.register(new RegisterRequest("Outro Nome", "joao.novo@example.com", "outrasenha789"))
        );

        assertEquals("login", exception.getCampo());
    }

    /**
     * Verifica que a senha e persistida somente em formato hasheado.
     * Entrada: request valido de cadastro.
     * Esperado: valor salvo em `passwd` diferente da senha em texto puro e compativel com Bcrypt.
     */
    @Test
    void shouldPersistHashedPasswordInsteadOfPlaintext() {
        authService.register(new RegisterRequest("Joao Novo", "joao.novo@example.com", "senhaforte456"));

        String storedPassword = jdbcClient.sql("SELECT passwd FROM users WHERE login = :login")
                .param("login", "joao.novo@example.com")
                .query(String.class)
                .single();

        assertNotNull(storedPassword);
        assertTrue(storedPassword.startsWith("$2"));
        assertTrue(new BCryptPasswordEncoder().matches("senhaforte456", storedPassword));
    }

    /**
     * Cria o DataSource H2 usado pelos testes do servico.
     * Estrategia: usa modo de compatibilidade PostgreSQL e mantem o banco em memoria ativo durante a JVM.
     * Efeitos colaterais: nenhum fora da criacao do objeto.
     *
     * @return DataSource apontando para um banco H2 em memoria.
     */
    private DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:auth_register_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
