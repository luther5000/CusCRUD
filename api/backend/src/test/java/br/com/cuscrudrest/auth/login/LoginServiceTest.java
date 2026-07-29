package br.com.cuscrudrest.auth.login;

import br.com.cuscrudrest.auth.jwt.JwtService;
import br.com.cuscrudrest.auth.jwt.ValidatedJwtToken;
import br.com.cuscrudrest.auth.support.EmailAddressValidator;
import br.com.cuscrudrest.auth.support.PasswordHasher;
import br.com.cuscrudrest.auth.user.UserRepository;
import br.com.cuscrudrest.common.error.UnauthenticatedException;
import br.com.cuscrudrest.common.error.ValidationException;
import br.com.cuscrudrest.config.CusCrudAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginServiceTest {

    private JdbcClient jdbcClient;
    private LoginService loginService;
    private JwtService jwtService;
    private PasswordHasher passwordHasher;

    /**
     * Prepara o schema minimo e as dependencias reais do servico de login.
     * Entrada: nenhuma.
     * Esperado: servico pronto para validar email, comparar senha e emitir JWT.
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

        passwordHasher = new PasswordHasher(new BCryptPasswordEncoder());
        UserRepository userRepository = new UserRepository(jdbcClient);
        jwtService = new JwtService(
                new CusCrudAuthProperties("test-secret-for-login-service", 3600),
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-03-24T18:00:00Z"), ZoneOffset.UTC)
        );

        loginService = new LoginService(
                new EmailAddressValidator(),
                passwordHasher,
                userRepository,
                jwtService
        );
    }

    /**
     * Verifica que o servico autentica credenciais validas e retorna token JWT.
     * Entrada: login existente e senha correta.
     * Esperado: token emitido, TTL fixo de 3600 e dados publicos do usuario.
     */
    @Test
    void shouldAuthenticateUserWhenCredentialsAreValid() {
        String encodedPassword = passwordHasher.hash("senhaforte456");
        jdbcClient.sql("""
                INSERT INTO users (name, login, passwd)
                VALUES (:name, :login, :passwd)
                """)
                .param("name", "Joao Novo")
                .param("login", "joao.novo@example.com")
                .param("passwd", encodedPassword)
                .update();

        LoginResponse response = loginService.login(new LoginRequest("joao.novo@example.com", "senhaforte456"));

        assertNotNull(response.token());
        assertEquals(3600, response.expiresIn());
        assertEquals("Joao Novo", response.user().name());
        assertEquals("joao.novo@example.com", response.user().login());
        assertNotNull(response.user().userId());
        assertNotNull(response.user().createdAt());

        ValidatedJwtToken validatedJwtToken = jwtService.validateToken(response.token());
        assertEquals(response.user().userId(), validatedJwtToken.userId());
        assertEquals(3600, validatedJwtToken.expiresIn());
    }

    /**
     * Verifica que o servico rejeita email fora do formato esperado.
     * Entrada: login malformado.
     * Esperado: ValidationException associada ao campo `login`.
     */
    @Test
    void shouldRejectMalformedEmail() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> loginService.login(new LoginRequest("joao.novoexample.com", "senhaforte456"))
        );

        assertEquals("login", exception.getCampo());
    }

    /**
     * Verifica que o servico rejeita login inexistente.
     * Entrada: email nao cadastrado na base.
     * Esperado: UnauthenticatedException associada ao campo `login`.
     */
    @Test
    void shouldRejectUnknownLogin() {
        UnauthenticatedException exception = assertThrows(
                UnauthenticatedException.class,
                () -> loginService.login(new LoginRequest("joao.novo@example.com", "senhaforte456"))
        );

        assertEquals("login", exception.getCampo());
        assertEquals("login not found", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita senha incorreta.
     * Entrada: login existente e senha diferente da persistida.
     * Esperado: UnauthenticatedException associada ao campo `passwd`.
     */
    @Test
    void shouldRejectIncorrectPassword() {
        jdbcClient.sql("""
                INSERT INTO users (name, login, passwd)
                VALUES (:name, :login, :passwd)
                """)
                .param("name", "Joao Novo")
                .param("login", "joao.novo@example.com")
                .param("passwd", passwordHasher.hash("senhaforte456"))
                .update();

        UnauthenticatedException exception = assertThrows(
                UnauthenticatedException.class,
                () -> loginService.login(new LoginRequest("joao.novo@example.com", "outrasenha789"))
        );

        assertEquals("passwd", exception.getCampo());
        assertEquals("incorrect password", exception.getInfo());
        assertTrue(exception.getMessage().contains("Credenciais invalidas"));
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
        dataSource.setUrl("jdbc:h2:mem:auth_login_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
