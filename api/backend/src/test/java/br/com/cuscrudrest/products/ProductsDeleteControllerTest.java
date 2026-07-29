package br.com.cuscrudrest.products;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "cuscrud.database.url=jdbc:h2:mem:delete_product_endpoint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "cuscrud.database.user=sa",
        "cuscrud.database.password=test",
        "cuscrud.auth.jwt-secret=test-jwt-secret-for-delete-product"
})
@AutoConfigureMockMvc
class ProductsDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JwtService jwtService;

    /**
     * Prepara o schema minimo antes de cada teste do endpoint de remocao de produtos.
     * Entrada: nenhuma.
     * Esperado: tabelas recriadas vazias com schema compativel com o contrato da API.
     */
    @BeforeEach
    void setUp() {
        jdbcClient.sql("DROP TABLE IF EXISTS products").update();
        jdbcClient.sql("DROP TABLE IF EXISTS types").update();
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
        jdbcClient.sql("""
                CREATE TABLE types (
                    type_id BIGINT PRIMARY KEY,
                    nome VARCHAR(255) NOT NULL,
                    imagem BYTEA NULL,
                    inv_id UUID NOT NULL,
                    CONSTRAINT uq_types_inv_type UNIQUE (inv_id, type_id)
                )
                """).update();
        jdbcClient.sql("""
                CREATE TABLE products (
                    product_id BIGINT PRIMARY KEY,
                    type_id BIGINT NOT NULL,
                    marca VARCHAR(255) NULL,
                    dataValidade TIMESTAMP WITH TIME ZONE NULL,
                    unidade BIGINT NULL,
                    unidadeMedida VARCHAR(255) NULL,
                    quantidade BIGINT NOT NULL DEFAULT 0,
                    inv_id UUID NOT NULL,
                    CONSTRAINT fk_products_types
                        FOREIGN KEY (inv_id, type_id)
                        REFERENCES types (inv_id, type_id)
                )
                """).update();
    }

    /**
     * Verifica que DELETE /api/v1/inventories/{inv_id}/products/{product_id} remove um produto quando o usuario possui escrita.
     * Entrada: JWT valido para usuario com `role = 1` e produto existente.
     * Esperado: status 204 sem corpo e produto removido da base.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldDeleteProductWhenUserHasWriteAccess() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque");
        insertInventoryAccess(userId, inventoryId, 1);
        insertType(10L, "Higiene", inventoryId);
        insertProduct(20L, 10L, inventoryId);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(delete("/inventories/{inv_id}/products/{product_id}", inventoryId, 20L)
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Integer remaining = jdbcClient.sql("SELECT COUNT(*) FROM products WHERE inv_id = :inventoryId AND product_id = :productId")
                .param("inventoryId", inventoryId)
                .param("productId", 20L)
                .query(Integer.class)
                .single();
        assertEquals(0, remaining);
    }

    /**
     * Verifica que DELETE /api/v1/inventories/{inv_id}/products/{product_id} exige autenticacao.
     * Entrada: requisicao sem Authorization.
     * Esperado: status 401 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldRequireAuthenticationToDeleteProduct() throws Exception {
        mockMvc.perform(delete("/inventories/{inv_id}/products/{product_id}", UUID.randomUUID(), 20L))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.details.campo").value("Authorization"));
    }

    /**
     * Verifica que DELETE /api/v1/inventories/{inv_id}/products/{product_id} rejeita usuario apenas com role reader.
     * Entrada: produto existente e JWT valido para usuario com `role = 2`.
     * Esperado: status 403 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnForbiddenWhenUserOnlyHasReaderAccess() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque");
        insertInventoryAccess(userId, inventoryId, 2);
        insertType(10L, "Higiene", inventoryId);
        insertProduct(20L, 10L, inventoryId);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(delete("/inventories/{inv_id}/products/{product_id}", inventoryId, 20L)
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.details.campo").value("inv_id"));
    }

    /**
     * Verifica que DELETE /api/v1/inventories/{inv_id}/products/{product_id} rejeita `product_id` inexistente.
     * Entrada: JWT valido para usuario com escrita no inventario e produto ausente.
     * Esperado: status 404 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque");
        insertInventoryAccess(userId, inventoryId, 1);
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(delete("/inventories/{inv_id}/products/{product_id}", inventoryId, 20L)
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.campo").value("product_id"));
    }

    /**
     * Verifica que DELETE /api/v1/inventories/{inv_id}/products/{product_id} rejeita inventario sem acesso para o usuario.
     * Entrada: JWT valido para usuario sem vinculo com o inventario.
     * Esperado: status 404 no formato padrao de erro.
     *
     * @throws Exception quando a execucao do request de teste falha.
     */
    @Test
    void shouldReturnNotFoundWhenInventoryDoesNotExistOrDoesNotBelongToUser() throws Exception {
        UUID userId = insertUser("Joao Novo", "joao.novo@example.com", "senhaforte456");
        IssuedJwtToken issuedJwtToken = jwtService.issueToken(userId);

        mockMvc.perform(delete("/inventories/{inv_id}/products/{product_id}", UUID.randomUUID(), 20L)
                        .header("Authorization", "Bearer " + issuedJwtToken.token()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.campo").value("inv_id"));
    }

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

    private void insertType(long typeId, String nome, UUID inventoryId) {
        jdbcClient.sql("""
                INSERT INTO types (type_id, nome, inv_id)
                VALUES (:typeId, :nome, :inventoryId)
                """)
                .param("typeId", typeId)
                .param("nome", nome)
                .param("inventoryId", inventoryId)
                .update();
    }

    private void insertProduct(long productId, long typeId, UUID inventoryId) {
        jdbcClient.sql("""
                INSERT INTO products (product_id, type_id, quantidade, inv_id)
                VALUES (:productId, :typeId, :quantidade, :inventoryId)
                """)
                .param("productId", productId)
                .param("typeId", typeId)
                .param("quantidade", 10L)
                .param("inventoryId", inventoryId)
                .update();
    }
}
