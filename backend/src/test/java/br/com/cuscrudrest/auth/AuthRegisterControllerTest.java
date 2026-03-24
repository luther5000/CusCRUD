package br.com.cuscrudrest.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "cuscrud.database.url=jdbc:h2:mem:auth_register_endpoint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "cuscrud.database.user=sa",
        "cuscrud.database.password=test"
})
@AutoConfigureMockMvc
class AuthRegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    /**
     * Prepara a tabela `users` antes de cada teste do endpoint de cadastro.
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
     * Verifica que POST /api/v1/auth/register cria um usuario valido e retorna 201.
     * Entrada: payload com nome, email unico e senha dentro do intervalo permitido.
     * Esperado: status 201 com body contendo `user_id`, `name`, `login` e `created_at`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldCreateUserWhenPayloadIsValid() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Joao Novo",
                                  "login": "joao.novo@example.com",
                                  "passwd": "senhaforte456"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user_id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Joao Novo"))
                .andExpect(jsonPath("$.login").value("joao.novo@example.com"))
                .andExpect(jsonPath("$.created_at").value(matchesPattern(
                        "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})$"
                )));
    }

    /**
     * Verifica que POST /api/v1/auth/register rejeita email fora do formato esperado.
     * Entrada: payload com `login` sem formato valido de email.
     * Esperado: status 400 no formato padrao de erro, com indicacao do campo `login`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnValidationErrorWhenEmailIsMalformed() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Joao Novo",
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
     * Verifica que POST /api/v1/auth/register rejeita payload com nome vazio.
     * Entrada: payload com `name` em branco.
     * Esperado: status 400 no formato padrao de erro, com indicacao do campo `name`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnValidationErrorWhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "login": "joao.novo@example.com",
                                  "passwd": "senhaforte456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.campo").value("name"));
    }

    /**
     * Verifica que POST /api/v1/auth/register retorna conflito quando o login ja existe.
     * Entrada: payload com email previamente persistido na tabela `users`.
     * Esperado: status 409 no formato padrao de erro, com indicacao do campo `login`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnConflictWhenLoginAlreadyExists() throws Exception {
        jdbcClient.sql("""
                INSERT INTO users (name, login, passwd)
                VALUES ('Joao Existente', 'joao.novo@example.com', '$2a$10$abcdefghijklmnopqrstuv')
                """)
                .update();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Joao Novo",
                                  "login": "joao.novo@example.com",
                                  "passwd": "senhaforte456"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.details.campo").value("login"));
    }
}
