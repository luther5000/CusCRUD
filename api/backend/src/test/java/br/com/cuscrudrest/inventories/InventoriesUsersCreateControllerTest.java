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
        "cuscrud.database.url=jdbc:h2:mem:create_inventory_user_endpoint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "cuscrud.database.user=sa",
        "cuscrud.database.password=test",
        "cuscrud.auth.jwt-secret=test-jwt-secret-for-create-inventory-user"
})
@AutoConfigureMockMvc
class InventoriesUsersCreateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JwtService jwtService;

    /**
     * Prepara o schema minimo antes de cada teste do endpoint de adicao de usuarios ao inventario.
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
     * Verifica que POST /api/v1/inventories/{inv_id}/users adiciona um usuario existente com role valida.
     * Entrada: JWT de owner, inventario existente e usuario alvo existente sem vinculo.
     * Esperado: status 201 com inventario e usuario retornados no corpo.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldAddExistingUserToInventoryWhenAuthenticatedUserIsOwner() throws Exception {
        UUID ownerId = insertUser(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "Joao Silva",
                "joao@example.com",
                "senhaforte456"
        );
        UUID targetUserId = insertUser(
                UUID.fromString("660e8400-e29b-41d4-a716-446655440000"),
                "Maria Editor",
                "maria.editor@example.com",
                "senhaforte456"
        );
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(post("/inventories/{inv_id}/users", inventoryId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "maria.editor@example.com",
                                  "role": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inventory.inv_id").value(inventoryId.toString()))
                .andExpect(jsonPath("$.inventory.inv_name").value("Estoque da Loja"))
                .andExpect(jsonPath("$.user.user_id").value(targetUserId.toString()))
                .andExpect(jsonPath("$.user.name").value("Maria Editor"))
                .andExpect(jsonPath("$.user.login").value("maria.editor@example.com"))
                .andExpect(jsonPath("$.user.role").value(1));
    }

    /**
     * Verifica que POST /api/v1/inventories/{inv_id}/users exige autenticacao.
     * Entrada: requisicao sem JWT.
     * Esperado: status 401 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldRequireAuthenticationToAddInventoryUser() throws Exception {
        mockMvc.perform(post("/inventories/{inv_id}/users", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "maria.editor@example.com",
                                  "role": 1
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.details.campo").value("Authorization"));
    }

    /**
     * Verifica que POST /api/v1/inventories/{inv_id}/users rejeita solicitante sem role owner.
     * Entrada: JWT valido para usuario com role `1` no inventario.
     * Esperado: status 403 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnForbiddenWhenAuthenticatedUserIsNotOwner() throws Exception {
        UUID editorId = insertUser("Maria Editor", "maria.editor@example.com", "senhaforte456");
        UUID targetUserId = insertUser("Carlos Reader", "carlos.reader@example.com", "senhaforte456");
        UUID ownerId = insertUser("Joao Silva", "joao@example.com", "senhaforte456");
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        insertInventoryAccess(editorId, inventoryId, 1);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(editorId);

        mockMvc.perform(post("/inventories/{inv_id}/users", inventoryId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "carlos.reader@example.com",
                                  "role": 2
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.details.campo").value("inv_id"));
    }

    /**
     * Verifica que POST /api/v1/inventories/{inv_id}/users rejeita login inexistente.
     * Entrada: owner autenticado e login nao cadastrado.
     * Esperado: status 404 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnNotFoundWhenTargetUserLoginDoesNotExist() throws Exception {
        UUID ownerId = insertUser("Joao Silva", "joao@example.com", "senhaforte456");
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(post("/inventories/{inv_id}/users", inventoryId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "inexistente@example.com",
                                  "role": 1
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.campo").value("login"));
    }

    /**
     * Verifica que POST /api/v1/inventories/{inv_id}/users rejeita role fora do conjunto permitido.
     * Entrada: owner autenticado e role `0` no payload.
     * Esperado: status 400 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnValidationErrorWhenRoleIsOutsideAllowedSet() throws Exception {
        UUID ownerId = insertUser("Joao Silva", "joao@example.com", "senhaforte456");
        UUID targetUserId = insertUser("Maria Editor", "maria.editor@example.com", "senhaforte456");
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(post("/inventories/{inv_id}/users", inventoryId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "maria.editor@example.com",
                                  "role": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.campo").value("role"));
    }

    /**
     * Verifica que POST /api/v1/inventories/{inv_id}/users rejeita usuario ja vinculado ao inventario.
     * Entrada: owner autenticado e usuario alvo ja presente em `inventory_access`.
     * Esperado: status 409 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnConflictWhenTargetUserAlreadyHasAccessToInventory() throws Exception {
        UUID ownerId = insertUser("Joao Silva", "joao@example.com", "senhaforte456");
        UUID targetUserId = insertUser("Maria Editor", "maria.editor@example.com", "senhaforte456");
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        insertInventoryAccess(targetUserId, inventoryId, 1);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(post("/inventories/{inv_id}/users", inventoryId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "maria.editor@example.com",
                                  "role": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.details.campo").value("login"));
    }

    private UUID insertUser(String name, String login, String rawPassword) {
        return insertUser(UUID.randomUUID(), name, login, rawPassword);
    }

    private UUID insertUser(UUID userId, String name, String login, String rawPassword) {
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

    private void insertInventory(UUID inventoryId, String inventoryName) {
        jdbcClient.sql("""
                INSERT INTO inventories (inv_id, inv_name)
                VALUES (:inventoryId, :inventoryName)
                """)
                .param("inventoryId", inventoryId)
                .param("inventoryName", inventoryName)
                .update();
    }

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
