package br.com.cuscrudrest.inventories;

import br.com.cuscrudrest.auth.jwt.IssuedJwtToken;
import br.com.cuscrudrest.auth.jwt.JwtService;
import br.com.cuscrudrest.auth.support.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "cuscrud.database.url=jdbc:h2:mem:create_inventory_endpoint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "cuscrud.database.user=sa",
        "cuscrud.database.password=test",
        "cuscrud.auth.jwt-secret=test-jwt-secret-for-create-inventory"
})
@AutoConfigureMockMvc
class InventoriesCreateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JwtService jwtService;

    /**
     * Prepara o schema minimo antes de cada teste do endpoint de criacao de inventario.
     * Entrada: nenhuma.
     * Esperado: tabelas recriadas vazias com schema compativel com o contrato da API.
     */
    @BeforeEach
    void setUp() {
        jdbcClient.sql("DROP TABLE IF EXISTS inventory_access").update();
        jdbcClient.sql("DROP TABLE IF EXISTS inventories").update();
        jdbcClient.sql("DROP TABLE IF EXISTS users").update();
        jdbcClient.sql("""
                CREATE TABLE users (
                    user_id UUID DEFAULT random_uuid() PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    login VARCHAR(255) UNIQUE NOT NULL,
                    passwd TEXT NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """).update();
        jdbcClient.sql("""
                CREATE TABLE inventories (
                    inv_id UUID PRIMARY KEY,
                    inv_name VARCHAR(255) NOT NULL
                )
                """).update();
        jdbcClient.sql("""
                CREATE TABLE inventory_access (
                    user_id UUID NOT NULL,
                    inv_id UUID NOT NULL,
                    role INT NOT NULL,
                    PRIMARY KEY (user_id, inv_id)
                )
                """).update();
    }

    /**
     * Verifica que POST /api/v1/inventories cria um inventario quando o JWT e valido.
     * Entrada: usuario autenticado e payload com nome valido.
     * Esperado: status 201 com `inventory.inv_id`, `inventory.inv_name` e `role = 0`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldCreateInventoryWhenPayloadAndJwtAreValid() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(post("/inventories")
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inv_name": "Estoque da Loja"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inventory.inv_id").isNotEmpty())
                .andExpect(jsonPath("$.inventory.inv_name").value("Estoque da Loja"))
                .andExpect(jsonPath("$.role").value(0));
    }

    /**
     * Verifica que POST /api/v1/inventories rejeita payload invalido.
     * Entrada: JWT valido e `inv_name` em branco.
     * Esperado: status 400 no formato padrao de erro com indicacao do campo `inventoryName`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnValidationErrorWhenInventoryNameIsBlank() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(post("/inventories")
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inv_name": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.campo").value("inventoryName"));
    }

    /**
     * Verifica que POST /api/v1/inventories exige autenticacao.
     * Entrada: requisicao sem JWT.
     * Esperado: status 401 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldRequireAuthenticationToCreateInventory() throws Exception {
        mockMvc.perform(post("/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inv_name": "Estoque da Loja"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.details.campo").value("Authorization"));
    }

    /**
     * Verifica que POST /api/v1/inventories rejeita criacao acima do limite de ownership.
     * Entrada: JWT valido para usuario com 100 inventarios owner ja persistidos.
     * Esperado: status 409 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnConflictWhenOwnerInventoryLimitIsReached() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        for (int index = 0; index < 100; index++) {
            UUID inventoryId = UUID.randomUUID();
            jdbcClient.sql("INSERT INTO inventories (inv_id, inv_name) VALUES (:inventoryId, :inventoryName)")
                    .param("inventoryId", inventoryId)
                    .param("inventoryName", "Inventario " + index)
                    .update();
            jdbcClient.sql("""
                    INSERT INTO inventory_access (user_id, inv_id, role)
                    VALUES (:userId, :inventoryId, 0)
                    """)
                    .param("userId", userId)
                    .param("inventoryId", inventoryId)
                    .update();
        }
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(post("/inventories")
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inv_name": "Estoque da Loja"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.details.campo").value("inventory"));
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
        UUID userId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO users (user_id, name, login, passwd)
                VALUES (:userId, :name, :login, :passwd)
                """)
                .param("userId", userId)
                .param("name", name)
                .param("login", login)
                .param("passwd", passwordHasher.hash(rawPassword))
                .update();
        return userId;
    }
}
