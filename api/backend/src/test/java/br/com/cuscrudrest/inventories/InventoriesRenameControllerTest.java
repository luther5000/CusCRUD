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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "cuscrud.database.url=jdbc:h2:mem:rename_inventory_endpoint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "cuscrud.database.user=sa",
        "cuscrud.database.password=test",
        "cuscrud.auth.jwt-secret=test-jwt-secret-for-rename-inventory"
})
@AutoConfigureMockMvc
class InventoriesRenameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JwtService jwtService;

    /**
     * Prepara o schema minimo antes de cada teste do endpoint de renomeacao de inventario.
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
     * Verifica que PATCH /api/v1/inventories/{inv_id} renomeia um inventario de owner autenticado.
     * Entrada: JWT valido para usuario com `role = 0` e payload com novo nome valido.
     * Esperado: status 200 com inventario atualizado e `role = 0`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldRenameInventoryWhenUserIsOwner() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque Antigo");
        insertInventoryAccess(userId, inventoryId, 0);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(patch("/inventories/{inv_id}", inventoryId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inv_name": "Estoque Renomeado"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inventory.inv_id").value(inventoryId.toString()))
                .andExpect(jsonPath("$.inventory.inv_name").value("Estoque Renomeado"))
                .andExpect(jsonPath("$.role").value(0));
    }

    /**
     * Verifica que PATCH /api/v1/inventories/{inv_id} exige autenticacao.
     * Entrada: requisicao sem Authorization.
     * Esperado: status 401 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldRequireAuthenticationToRenameInventory() throws Exception {
        mockMvc.perform(patch("/inventories/{inv_id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inv_name": "Estoque Renomeado"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.details.campo").value("Authorization"));
    }

    /**
     * Verifica que PATCH /api/v1/inventories/{inv_id} rejeita payload invalido.
     * Entrada: JWT valido e payload com `inv_name` em branco.
     * Esperado: status 400 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnValidationErrorWhenInventoryNameIsBlank() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque Antigo");
        insertInventoryAccess(userId, inventoryId, 0);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(patch("/inventories/{inv_id}", inventoryId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inv_name": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.campo").value("inventoryName"));
    }

    /**
     * Verifica que PATCH /api/v1/inventories/{inv_id} rejeita inventario inexistente.
     * Entrada: JWT valido para usuario autenticado e `inv_id` sem registro.
     * Esperado: status 404 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnNotFoundWhenInventoryDoesNotExist() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(patch("/inventories/{inv_id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inv_name": "Estoque Renomeado"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.campo").value("inv_id"));
    }

    /**
     * Verifica que PATCH /api/v1/inventories/{inv_id} rejeita usuario sem vinculo ao inventario.
     * Entrada: inventario existente sem registro de acesso para o usuario autenticado.
     * Esperado: status 404 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnNotFoundWhenUserDoesNotBelongToInventory() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque Antigo");
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(patch("/inventories/{inv_id}", inventoryId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inv_name": "Estoque Renomeado"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.campo").value("inv_id"));
    }

    /**
     * Verifica que PATCH /api/v1/inventories/{inv_id} rejeita usuario autenticado sem role owner.
     * Entrada: inventario existente com vinculo `role = 1` para o usuario autenticado.
     * Esperado: status 403 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnForbiddenWhenUserIsNotOwner() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque Antigo");
        insertInventoryAccess(userId, inventoryId, 1);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(patch("/inventories/{inv_id}", inventoryId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inv_name": "Estoque Renomeado"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.details.campo").value("inv_id"));
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

    /**
     * Persiste um inventario de teste.
     *
     * @param inventoryId identificador do inventario.
     * @param inventoryName nome do inventario.
     */
    private void insertInventory(UUID inventoryId, String inventoryName) {
        jdbcClient.sql("""
                INSERT INTO inventories (inv_id, inv_name)
                VALUES (:inventoryId, :inventoryName)
                """)
                .param("inventoryId", inventoryId)
                .param("inventoryName", inventoryName)
                .update();
    }

    /**
     * Persiste um vinculo de acesso de teste.
     *
     * @param userId identificador do usuario.
     * @param inventoryId identificador do inventario.
     * @param role role do usuario no inventario.
     */
    private void insertInventoryAccess(UUID userId, UUID inventoryId, int role) {
        jdbcClient.sql("""
                INSERT INTO inventory_access (user_id, inv_id, role)
                VALUES (:userId, :inventoryId, :role)
                """)
                .param("userId", userId)
                .param("inventoryId", inventoryId)
                .param("role", role)
                .update();
    }
}
