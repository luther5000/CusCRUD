package br.com.cuscrudrest.auth;

import br.com.cuscrudrest.auth.jwt.IssuedJwtToken;
import br.com.cuscrudrest.auth.jwt.JwtService;
import br.com.cuscrudrest.auth.support.PasswordHasher;
import br.com.cuscrudrest.config.CusCrudAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "cuscrud.database.url=jdbc:h2:mem:auth_validate_endpoint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "cuscrud.database.user=sa",
        "cuscrud.database.password=test",
        "cuscrud.auth.jwt-secret=test-jwt-secret-for-validate"
})
@AutoConfigureMockMvc
class AuthValidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JwtService jwtService;

    /**
     * Prepara a tabela `users` antes de cada teste do endpoint de validacao.
     * Entrada: nenhuma.
     * Esperado: tabela recriada vazia com schema compativel com o contrato da API.
     */
    @BeforeEach
    void setUp() {
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
    }

    /**
     * Verifica que GET /api/v1/auth/validate valida um token correto.
     * Entrada: Bearer token assinado para usuario existente.
     * Esperado: status 200 com os dados publicos do usuario e metadados do token.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldValidateTokenAndReturnAuthenticatedUser() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(get("/auth/validate")
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user.user_id").value(userId.toString()))
                .andExpect(jsonPath("$.user.name").value("Joao Novo"))
                .andExpect(jsonPath("$.user.login").value("joao.novo@example.com"))
                .andExpect(jsonPath("$.user.created_at").value(matchesPattern(
                        "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})$"
                )))
                .andExpect(jsonPath("$.token.expires_in").value(3600))
                .andExpect(jsonPath("$.token.issued_at").value(matchesPattern(
                        "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})$"
                )));
    }

    /**
     * Verifica que GET /api/v1/auth/validate rejeita ausencia do header Authorization.
     * Entrada: requisicao sem token Bearer.
     * Esperado: status 401 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/auth/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.details.campo").value("Authorization"));
    }

    /**
     * Verifica que GET /api/v1/auth/validate rejeita token expirado.
     * Entrada: Bearer token emitido no passado com o mesmo segredo da aplicacao.
     * Esperado: status 401 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnUnauthorizedWhenTokenIsExpired() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        JwtService expiredTokenJwtService = new JwtService(
                new CusCrudAuthProperties("test-jwt-secret-for-validate", 3600),
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-03-24T10:00:00Z"), ZoneOffset.UTC)
        );
        IssuedJwtToken expiredJwtToken = expiredTokenJwtService.issueToken(userId);

        mockMvc.perform(get("/auth/validate")
                        .header("Authorization", "Bearer " + expiredJwtToken.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.details.campo").value("Authorization"));
    }

    /**
     * Persiste um usuario de teste e devolve seu `user_id`.
     *
     * @param name nome do usuario.
     * @param login email do usuario.
     * @param rawPassword senha em texto puro.
     * @return identificador do usuario criado.
     */
    private UUID insertUser(String name, String login, String rawPassword) {
        jdbcClient.sql("""
                INSERT INTO users (name, login, passwd)
                VALUES (:name, :login, :passwd)
                """)
                .param("name", name)
                .param("login", login)
                .param("passwd", passwordHasher.hash(rawPassword))
                .update();

        return jdbcClient.sql("SELECT user_id FROM users WHERE login = :login")
                .param("login", login)
                .query(UUID.class)
                .single();
    }
}
