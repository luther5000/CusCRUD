package br.com.cuscrudrest.inventories;

import br.com.cuscrudrest.auth.jwt.IssuedJwtToken;
import br.com.cuscrudrest.auth.jwt.JwtService;
import br.com.cuscrudrest.auth.support.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "cuscrud.database.url=jdbc:h2:mem:delete_inventory_user_endpoint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "cuscrud.database.user=sa",
        "cuscrud.database.password=test",
        "cuscrud.auth.jwt-secret=test-jwt-secret-for-delete-inventory-user"
})
@AutoConfigureMockMvc
class InventoriesUsersDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JwtService jwtService;

    /**
     * Prepara o schema minimo antes de cada teste do endpoint de remocao de usuarios do inventario.
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
     * Verifica que DELETE /api/v1/inventories/{inv_id}/users/{user_id} remove um usuario vinculado.
     * Entrada: owner autenticado e usuario alvo com acesso ao inventario.
     * Esperado: status 204 sem corpo.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldDeleteInventoryUserWhenAuthenticatedUserIsOwner() throws Exception {
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
        insertInventoryAccess(targetUserId, inventoryId, 1);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(delete("/inventories/{inv_id}/users/{user_id}", inventoryId, targetUserId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    /**
     * Verifica que DELETE /api/v1/inventories/{inv_id}/users/{user_id} exige autenticacao.
     * Entrada: requisicao sem JWT.
     * Esperado: status 401 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldRequireAuthenticationToDeleteInventoryUser() throws Exception {
        mockMvc.perform(delete("/inventories/{inv_id}/users/{user_id}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.details.campo").value("Authorization"));
    }

    /**
     * Verifica que DELETE /api/v1/inventories/{inv_id}/users/{user_id} rejeita solicitante sem role owner.
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
        insertInventoryAccess(targetUserId, inventoryId, 2);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(editorId);

        mockMvc.perform(delete("/inventories/{inv_id}/users/{user_id}", inventoryId, targetUserId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.details.campo").value("inv_id"));
    }

    /**
     * Verifica que DELETE /api/v1/inventories/{inv_id}/users/{user_id} rejeita usuario alvo inexistente.
     * Entrada: owner autenticado e `user_id` ausente da base.
     * Esperado: status 404 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnNotFoundWhenTargetUserDoesNotExist() throws Exception {
        UUID ownerId = insertUser("Joao Silva", "joao@example.com", "senhaforte456");
        UUID targetUserId = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(delete("/inventories/{inv_id}/users/{user_id}", inventoryId, targetUserId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.campo").value("user_id"));
    }

    /**
     * Verifica que DELETE /api/v1/inventories/{inv_id}/users/{user_id} rejeita usuario sem vinculo no inventario.
     * Entrada: owner autenticado e usuario alvo existente sem acesso ao inventario.
     * Esperado: status 404 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnNotFoundWhenTargetUserDoesNotBelongToInventory() throws Exception {
        UUID ownerId = insertUser("Joao Silva", "joao@example.com", "senhaforte456");
        UUID targetUserId = insertUser("Maria Editor", "maria.editor@example.com", "senhaforte456");
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(delete("/inventories/{inv_id}/users/{user_id}", inventoryId, targetUserId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.campo").value("user_id"));
    }

    /**
     * Verifica que DELETE /api/v1/inventories/{inv_id}/users/{user_id} rejeita auto-remocao do owner.
     * Entrada: owner autenticado como alvo da remocao.
     * Esperado: status 409 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnConflictWhenOwnerAttemptsToRemoveOwnAccess() throws Exception {
        UUID ownerId = insertUser("Joao Silva", "joao@example.com", "senhaforte456");
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(delete("/inventories/{inv_id}/users/{user_id}", inventoryId, ownerId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.details.campo").value("user_id"));
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
