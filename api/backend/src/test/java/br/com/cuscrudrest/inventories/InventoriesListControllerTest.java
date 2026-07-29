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
        "cuscrud.database.url=jdbc:h2:mem:list_inventories_endpoint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "cuscrud.database.user=sa",
        "cuscrud.database.password=test",
        "cuscrud.auth.jwt-secret=test-jwt-secret-for-list-inventories"
})
@AutoConfigureMockMvc
class InventoriesListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JwtService jwtService;

    /**
     * Prepara o schema minimo antes de cada teste do endpoint de listagem de inventarios.
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
     * Verifica que GET /api/v1/inventories lista os inventarios acessiveis ao usuario autenticado.
     * Entrada: JWT valido para usuario com dois inventarios acessiveis.
     * Esperado: status 200 com itens ordenados e sem `next_page` quando nao houver mais resultados.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldListAccessibleInventories() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        UUID inventoryId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID inventoryId2 = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId2, "Deposito Central");
        insertInventory(inventoryId1, "Estoque da Loja");
        insertInventoryAccess(userId, inventoryId2, 2);
        insertInventoryAccess(userId, inventoryId1, 0);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(get("/inventories")
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inventories[0].inv_id").value(inventoryId1.toString()))
                .andExpect(jsonPath("$.inventories[0].inv_name").value("Estoque da Loja"))
                .andExpect(jsonPath("$.inventories[0].role").value(0))
                .andExpect(jsonPath("$.inventories[1].inv_id").value(inventoryId2.toString()))
                .andExpect(jsonPath("$.inventories[1].role").value(2))
                .andExpect(jsonPath("$.next_page").doesNotExist());
    }

    /**
     * Verifica que GET /api/v1/inventories inclui `next_page` quando houver mais resultados.
     * Entrada: JWT valido para usuario com dois inventarios acessiveis e query `limit = 1`.
     * Esperado: status 200 com um item e `next_page` apontando para `offset=1&limit=1`.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldIncludeNextPageWhenThereAreMoreResults() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        UUID inventoryId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID inventoryId2 = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId1, "Primeiro");
        insertInventory(inventoryId2, "Segundo");
        insertInventoryAccess(userId, inventoryId1, 0);
        insertInventoryAccess(userId, inventoryId2, 2);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(get("/inventories")
                        .queryParam("limit", "1")
                        .queryParam("offset", "0")
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventories.length()").value(1))
                .andExpect(jsonPath("$.next_page").value(endsWith("/inventories?offset=1&limit=1")));
    }

    /**
     * Verifica que GET /api/v1/inventories exige autenticacao.
     * Entrada: requisicao sem Authorization.
     * Esperado: status 401 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldRequireAuthenticationToListInventories() throws Exception {
        mockMvc.perform(get("/inventories"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.details.campo").value("Authorization"));
    }

    /**
     * Verifica que GET /api/v1/inventories rejeita `offset` apos o fim da lista.
     * Entrada: JWT valido para usuario com um inventario acessivel e query `offset = 1`.
     * Esperado: status 404 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnNotFoundWhenOffsetIsAfterEndOfList() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Primeiro");
        insertInventoryAccess(userId, inventoryId, 0);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(get("/inventories")
                        .queryParam("offset", "1")
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.campo").value("offset"));
    }

    /**
     * Verifica que GET /api/v1/inventories rejeita `limit` fora do intervalo aceito.
     * Entrada: JWT valido para usuario autenticado e query `limit = 201`.
     * Esperado: status 404 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnNotFoundWhenLimitIsOutsideAcceptedRange() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(get("/inventories")
                        .queryParam("limit", "201")
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.campo").value("limit"));
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
