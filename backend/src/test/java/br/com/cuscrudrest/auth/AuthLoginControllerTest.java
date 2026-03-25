package br.com.cuscrudrest.auth;

import br.com.cuscrudrest.auth.support.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "cuscrud.database.url=jdbc:h2:mem:auth_login_endpoint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "cuscrud.database.user=sa",
        "cuscrud.database.password=test"
})
@AutoConfigureMockMvc
class AuthLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Prepara a tabela `users` antes de cada teste do endpoint de login.
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
     * Verifica que POST /api/v1/auth/login autentica um usuario valido e retorna 200.
     * Entrada: login existente e senha correta.
     * Esperado: token JWT, TTL fixo e dados publicos do usuario autenticado.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldAuthenticateUserWhenCredentialsAreValid() throws Exception {
        jdbcClient.sql("""
                INSERT INTO users (name, login, passwd)
                VALUES (:name, :login, :passwd)
                """)
                .param("name", "Joao Novo")
                .param("login", "joao.novo@example.com")
                .param("passwd", new PasswordHasher(passwordEncoder).hash("senhaforte456"))
                .update();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "joao.novo@example.com",
                                  "passwd": "senhaforte456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(matchesPattern("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andExpect(jsonPath("$.user.user_id").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value("Joao Novo"))
                .andExpect(jsonPath("$.user.login").value("joao.novo@example.com"))
                .andExpect(jsonPath("$.user.created_at").value(matchesPattern(
                        "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})$"
                )));
    }

    /**
     * Verifica que POST /api/v1/auth/login rejeita email malformado.
     * Entrada: payload com `login` fora do formato de email.
     * Esperado: status 400 no formato padrao de erro, com indicacao do campo `login`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnValidationErrorWhenEmailIsMalformed() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "joao.novoexample.com",
                                  "passwd": "senhaforte456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.campo").value("login"));
    }

    /**
     * Verifica que POST /api/v1/auth/login rejeita login inexistente.
     * Entrada: email nao cadastrado na base.
     * Esperado: status 401 no formato padrao de erro, com indicacao do campo `login`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnUnauthorizedWhenLoginDoesNotExist() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "joao.novo@example.com",
                                  "passwd": "senhaforte456"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.details.campo").value("login"));
    }

    /**
     * Verifica que POST /api/v1/auth/login rejeita senha incorreta.
     * Entrada: login existente e senha diferente da persistida.
     * Esperado: status 401 no formato padrao de erro, com indicacao do campo `passwd`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnUnauthorizedWhenPasswordIsIncorrect() throws Exception {
        jdbcClient.sql("""
                INSERT INTO users (name, login, passwd)
                VALUES (:name, :login, :passwd)
                """)
                .param("name", "Joao Novo")
                .param("login", "joao.novo@example.com")
                .param("passwd", new PasswordHasher(passwordEncoder).hash("senhaforte456"))
                .update();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "joao.novo@example.com",
                                  "passwd": "outrasenha789"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.details.campo").value("passwd"));
    }
}
