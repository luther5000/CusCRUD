package br.com.cuscrudrest.auth.validate;

import br.com.cuscrudrest.auth.jwt.IssuedJwtToken;
import br.com.cuscrudrest.auth.jwt.JwtService;
import br.com.cuscrudrest.auth.support.PasswordHasher;
import br.com.cuscrudrest.auth.user.UserRepository;
import br.com.cuscrudrest.common.error.UnauthenticatedException;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidateServiceTest {

    private JdbcClient jdbcClient;
    private JwtService jwtService;
    private ValidateService validateService;

    /**
     * Prepara o schema minimo e as dependencias reais do servico de validacao.
     * Entrada: nenhuma.
     * Esperado: servico pronto para validar Bearer token e recarregar o usuario autenticado.
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

        jwtService = new JwtService(
                new CusCrudAuthProperties("test-secret-for-validate-service", 3600),
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-03-24T18:00:00Z"), ZoneOffset.UTC)
        );

        validateService = new ValidateService(jwtService, new UserRepository(jdbcClient));
    }

    /**
     * Verifica que o servico valida um token correto e retorna usuario + metadados.
     * Entrada: Bearer token emitido para um usuario persistido.
     * Esperado: dados publicos do usuario e `issued_at`/`expires_in` consistentes com o JWT.
     */
    @Test
    void shouldValidateTokenAndReturnUserData() {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        ValidateResponse response = validateService.validate("Bearer " + issuedJwtToken.token());

        assertEquals(userId, response.user().userId());
        assertEquals("Joao Novo", response.user().name());
        assertEquals("joao.novo@example.com", response.user().login());
        assertNotNull(response.user().createdAt());
        assertEquals(3600, response.token().expiresIn());
        assertEquals(issuedJwtToken.issuedAt(), response.token().issuedAt());
    }

    /**
     * Verifica que o servico rejeita ausencia do header Authorization.
     * Entrada: header nulo.
     * Esperado: UnauthenticatedException associada ao campo `Authorization`.
     */
    @Test
    void shouldRejectMissingAuthorizationHeader() {
        UnauthenticatedException exception = assertThrows(
                UnauthenticatedException.class,
                () -> validateService.validate(null)
        );

        assertEquals("Authorization", exception.getCampo());
    }

    /**
     * Verifica que o servico rejeita token cujo `sub` aponta para usuario inexistente.
     * Entrada: token valido assinado para UUID nao presente na base.
     * Esperado: UnauthenticatedException associada ao campo `Authorization`.
     */
    @Test
    void shouldRejectTokenWhenSubjectUserDoesNotExist() {
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(UUID.randomUUID());

        UnauthenticatedException exception = assertThrows(
                UnauthenticatedException.class,
                () -> validateService.validate("Bearer " + issuedJwtToken.token())
        );

        assertEquals("Authorization", exception.getCampo());
        assertEquals("jwt subject user not found", exception.getInfo());
    }

    /**
     * Persiste um usuario de teste e devolve seu `user_id`.
     *
     * @param name nome do usuario.
     * @param login email do usuario.
     * @param rawPassword senha em texto puro que sera hasheada para persistencia.
     * @return identificador do usuario criado.
     */
    private UUID insertUser(String name, String login, String rawPassword) {
        jdbcClient.sql("""
                INSERT INTO users (name, login, passwd)
                VALUES (:name, :login, :passwd)
                """)
                .param("name", name)
                .param("login", login)
                .param("passwd", new PasswordHasher(new BCryptPasswordEncoder()).hash(rawPassword))
                .update();

        return jdbcClient.sql("SELECT user_id FROM users WHERE login = :login")
                .param("login", login)
                .query(UUID.class)
                .single();
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
        dataSource.setUrl("jdbc:h2:mem:auth_validate_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
