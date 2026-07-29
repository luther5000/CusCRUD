package br.com.cuscrudrest.products.delete;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ForbiddenException;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.products.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteProductServiceTest {

    private JdbcClient jdbcClient;
    private DeleteProductService deleteProductService;
    private AuthenticatedUserPrincipal authenticatedUser;

    /**
     * Prepara o schema minimo e as dependencias reais do servico de remocao de produtos.
     * Entrada: nenhuma.
     * Esperado: servico pronto para remover produtos quando o usuario possuir escrita.
     */
    @BeforeEach
    void setUp() {
        DataSource dataSource = createDataSource();
        jdbcClient = JdbcClient.create(dataSource);
        jdbcClient.sql("DROP TABLE IF EXISTS products").update();
        jdbcClient.sql("DROP TABLE IF EXISTS types").update();
        jdbcClient.sql("DROP TABLE IF EXISTS inventory_access").update();
        jdbcClient.sql("DROP TABLE IF EXISTS inventories").update();
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

        UUID userId = UUID.randomUUID();
        authenticatedUser = new AuthenticatedUserPrincipal(
                userId,
                "Joao Novo",
                "joao.novo@example.com",
                OffsetDateTime.parse("2026-03-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-03-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-03-25T11:00:00-03:00"),
                3600
        );
        ProductRepository productRepository = new ProductRepository(
                jdbcClient,
                new NamedParameterJdbcTemplate(dataSource)
        );
        deleteProductService = new DeleteProductService(
                new InventoryAccessService(new br.com.cuscrudrest.inventories.InventoryRepository(jdbcClient)),
                productRepository
        );
    }

    /**
     * Verifica que o servico remove o produto quando o usuario possui escrita no inventario.
     * Entrada: produto existente e usuario com `role = 1`.
     * Esperado: linha removida da tabela `products`.
     */
    @Test
    void shouldDeleteProductWhenUserHasWriteAccess() {
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 1);
        insertType(10L, "Higiene", inventoryId);
        insertProduct(20L, 10L, inventoryId);

        deleteProductService.deleteProduct(authenticatedUser, inventoryId, 20L);

        Integer remaining = jdbcClient.sql("SELECT COUNT(*) FROM products WHERE inv_id = :inventoryId AND product_id = :productId")
                .param("inventoryId", inventoryId)
                .param("productId", 20L)
                .query(Integer.class)
                .single();

        assertEquals(0, remaining);
    }

    /**
     * Verifica que o servico rejeita `product_id` inexistente.
     * Entrada: inventario acessivel sem produto correspondente.
     * Esperado: NotFoundException associada ao campo `product_id`.
     */
    @Test
    void shouldRejectWhenProductDoesNotExist() {
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 1);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> deleteProductService.deleteProduct(authenticatedUser, inventoryId, 20L)
        );

        assertEquals("product_id", exception.getCampo());
        assertEquals("product not found", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita usuario sem permissao de escrita no inventario.
     * Entrada: produto existente e usuario com `role = 2`.
     * Esperado: ForbiddenException associada ao campo `inv_id`.
     */
    @Test
    void shouldRejectWhenUserCannotWrite() {
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 2);
        insertType(10L, "Higiene", inventoryId);
        insertProduct(20L, 10L, inventoryId);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> deleteProductService.deleteProduct(authenticatedUser, inventoryId, 20L)
        );

        assertEquals("inv_id", exception.getCampo());
        assertEquals("write role required", exception.getInfo());
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

    /**
     * Cria a fonte de dados H2 para o teste de servico.
     *
     * @return DataSource configurado em modo PostgreSQL.
     */
    private DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:delete_product_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("test");
        return dataSource;
    }
}
