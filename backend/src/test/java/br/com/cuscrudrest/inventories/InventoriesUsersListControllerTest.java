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

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "cuscrud.database.url=jdbc:h2:mem:list_inventory_users_endpoint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "cuscrud.database.user=sa",
        "cuscrud.database.password=test",
        "cuscrud.auth.jwt-secret=test-jwt-secret-for-list-inventory-users"
})
@AutoConfigureMockMvc
class InventoriesUsersListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JwtService jwtService;

    /**
     * Prepara o schema minimo antes de cada teste do endpoint de listagem de usuarios do inventario.
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
     * Verifica que GET /api/v1/inventories/{inv_id}/users lista os usuarios quando o solicitante e owner.
     * Entrada: JWT valido para owner e inventario com owner, editor e reader.
     * Esperado: status 200 com inventario, usuarios ordenados por `user_id ASC` e sem `next_page` no fim da lista.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldListInventoryUsersWhenAuthenticatedUserIsOwner() throws Exception {
        UUID ownerId = insertUser(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "Joao Silva",
                "joao@example.com",
                "senhaforte456"
        );
        UUID editorId = insertUser(
                UUID.fromString("660e8400-e29b-41d4-a716-446655440000"),
                "Maria Editor",
                "maria.editor@example.com",
                "senhaforte456"
        );
        UUID readerId = insertUser(
                UUID.fromString("770e8400-e29b-41d4-a716-446655440000"),
                "Carlos Reader",
                "carlos.reader@example.com",
                "senhaforte456"
        );
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        insertInventoryAccess(editorId, inventoryId, 1);
        insertInventoryAccess(readerId, inventoryId, 2);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(get("/inventories/{inv_id}/users", inventoryId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inventory.inv_id").value(inventoryId.toString()))
                .andExpect(jsonPath("$.inventory.inv_name").value("Estoque da Loja"))
                .andExpect(jsonPath("$.users[0].user_id").value(ownerId.toString()))
                .andExpect(jsonPath("$.users[0].role").value(0))
                .andExpect(jsonPath("$.users[1].user_id").value(editorId.toString()))
                .andExpect(jsonPath("$.users[1].role").value(1))
                .andExpect(jsonPath("$.users[2].user_id").value(readerId.toString()))
                .andExpect(jsonPath("$.users[2].role").value(2))
                .andExpect(jsonPath("$.next_page").doesNotExist());
    }

    /**
     * Verifica que GET /api/v1/inventories/{inv_id}/users inclui `next_page` quando houver mais resultados.
     * Entrada: inventario com dois usuarios e query `limit = 1`.
     * Esperado: status 200 com um item e `next_page` apontando para `offset=1&limit=1`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldIncludeNextPageWhenThereAreMoreInventoryUsers() throws Exception {
        UUID ownerId = insertUser(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "Joao Silva",
                "joao@example.com",
                "senhaforte456"
        );
        UUID editorId = insertUser(
                UUID.fromString("660e8400-e29b-41d4-a716-446655440000"),
                "Maria Editor",
                "maria.editor@example.com",
                "senhaforte456"
        );
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        insertInventoryAccess(editorId, inventoryId, 1);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(get("/inventories/{inv_id}/users", inventoryId)
                        .queryParam("limit", "1")
                        .queryParam("offset", "0")
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(1))
                .andExpect(jsonPath("$.next_page").value(
                        endsWith("/inventories/" + inventoryId + "/users?offset=1&limit=1")
                ));
    }

    /**
     * Verifica que GET /api/v1/inventories/{inv_id}/users exige autenticacao.
     * Entrada: requisicao sem Authorization.
     * Esperado: status 401 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldRequireAuthenticationToListInventoryUsers() throws Exception {
        mockMvc.perform(get("/inventories/{inv_id}/users", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.details.campo").value("Authorization"));
    }

    /**
     * Verifica que GET /api/v1/inventories/{inv_id}/users rejeita usuario autenticado sem role owner.
     * Entrada: inventario existente com vinculo `role = 1` para o solicitante.
     * Esperado: status 403 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnForbiddenWhenAuthenticatedUserIsNotOwner() throws Exception {
        UUID editorId = insertUser("Maria Editor", "maria.editor@example.com", "senhaforte456");
        UUID ownerId = insertUser("Joao Silva", "joao@example.com", "senhaforte456");
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        insertInventoryAccess(editorId, inventoryId, 1);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(editorId);

        mockMvc.perform(get("/inventories/{inv_id}/users", inventoryId)
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.details.campo").value("inv_id"));
    }

    /**
     * Verifica que GET /api/v1/inventories/{inv_id}/users rejeita inventario inexistente ou sem acesso.
     * Entrada: JWT valido para usuario sem vinculo ao `inv_id`.
     * Esperado: status 404 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnNotFoundWhenInventoryDoesNotExistOrDoesNotBelongToUser() throws Exception {
        UUID ownerId = insertUser("Joao Silva", "joao@example.com", "senhaforte456");
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(get("/inventories/{inv_id}/users", UUID.randomUUID())
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.campo").value("inv_id"));
    }

    /**
     * Verifica que GET /api/v1/inventories/{inv_id}/users rejeita `offset` apos o fim da lista.
     * Entrada: inventario com um unico usuario e query `offset = 1`.
     * Esperado: status 404 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnNotFoundWhenOffsetIsAfterEndOfList() throws Exception {
        UUID ownerId = insertUser("Joao Silva", "joao@example.com", "senhaforte456");
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(ownerId, inventoryId, 0);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(ownerId);

        mockMvc.perform(get("/inventories/{inv_id}/users", inventoryId)
                        .queryParam("offset", "1")
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.campo").value("offset"));
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
        return insertUser(UUID.randomUUID(), name, login, rawPassword);
    }

    /**
     * Persiste um usuario de teste com identificador explicito e devolve seu `user_id`.
     *
     * @param userId identificador do usuario.
     * @param name nome do usuario.
     * @param login email do usuario.
     * @param rawPassword senha em texto puro.
     * @return identificador do usuario criado.
     */
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
