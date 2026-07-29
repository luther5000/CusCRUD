package br.com.cuscrudrest.types.delete;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.common.error.ForbiddenException;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.types.TypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteTypeServiceTest {

    private JdbcClient jdbcClient;
    private DeleteTypeService deleteTypeService;
    private AuthenticatedUserPrincipal authenticatedUser;

    /**
     * Prepara o schema minimo e as dependencias reais do servico de remocao de tipos.
     * Entrada: nenhuma.
     * Esperado: servico pronto para remover tipos quando o usuario possuir escrita.
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
                    nome VARCHAR(255) NOT NULL,
                    inv_id UUID NOT NULL,
                    type_id BIGINT NOT NULL,
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
        TypeRepository typeRepository = new TypeRepository(jdbcClient);
        deleteTypeService = new DeleteTypeService(
                new InventoryAccessService(new br.com.cuscrudrest.inventories.InventoryRepository(jdbcClient)),
                typeRepository
        );
    }

    /**
     * Verifica que o servico remove o tipo quando o usuario possui escrita no inventario.
     * Entrada: tipo existente sem produtos vinculados e usuario com `role = 1`.
     * Esperado: linha removida da tabela `types`.
     */
    @Test
    void shouldDeleteTypeWhenUserHasWriteAccess() {
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 1);
        insertType(10L, "Higiene", inventoryId);

        deleteTypeService.deleteType(authenticatedUser, inventoryId, 10L);

        Integer remaining = jdbcClient.sql("SELECT COUNT(*) FROM types WHERE inv_id = :inventoryId AND type_id = :typeId")
                .param("inventoryId", inventoryId)
                .param("typeId", 10L)
                .query(Integer.class)
                .single();

        assertEquals(0, remaining);
    }

    /**
     * Verifica que o servico rejeita `type_id` inexistente.
     * Entrada: inventario acessivel sem tipo correspondente.
     * Esperado: NotFoundException associada ao campo `type_id`.
     */
    @Test
    void shouldRejectWhenTypeDoesNotExist() {
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 1);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> deleteTypeService.deleteType(authenticatedUser, inventoryId, 10L)
        );

        assertEquals("type_id", exception.getCampo());
        assertEquals("type not found", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita usuario sem permissao de escrita no inventario.
     * Entrada: tipo existente e usuario com `role = 2`.
     * Esperado: ForbiddenException associada ao campo `inv_id`.
     */
    @Test
    void shouldRejectWhenUserCannotWrite() {
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 2);
        insertType(10L, "Higiene", inventoryId);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> deleteTypeService.deleteType(authenticatedUser, inventoryId, 10L)
        );

        assertEquals("inv_id", exception.getCampo());
        assertEquals("write role required", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita a remocao quando existem produtos associados ao tipo.
     * Entrada: tipo existente com produto vinculado pelo schema.
     * Esperado: ConflictException associada ao campo `type_id`.
     */
    @Test
    void shouldRejectWhenTypeHasAssociatedProducts() {
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 0);
        insertType(10L, "Higiene", inventoryId);
        insertProduct(20L, "Sabonete", inventoryId, 10L);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> deleteTypeService.deleteType(authenticatedUser, inventoryId, 10L)
        );

        assertEquals("type_id", exception.getCampo());
        assertEquals("type has associated products", exception.getInfo());
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

    /**
     * Persiste um tipo de teste.
     *
     * @param typeId identificador do tipo.
     * @param nome nome do tipo.
     * @param inventoryId identificador do inventario.
     */
    private void insertType(long typeId, String nome, UUID inventoryId) {
        jdbcClient.sql("""
                INSERT INTO types (type_id, nome, imagem, inv_id)
                VALUES (:typeId, :nome, null, :inventoryId)
                """)
                .param("typeId", typeId)
                .param("nome", nome)
                .param("inventoryId", inventoryId)
                .update();
    }

    /**
     * Persiste um produto de teste vinculado a um tipo.
     *
     * @param productId identificador do produto.
     * @param nome nome do produto.
     * @param inventoryId identificador do inventario.
     * @param typeId identificador do tipo associado.
     */
    private void insertProduct(long productId, String nome, UUID inventoryId, long typeId) {
        jdbcClient.sql("""
                INSERT INTO products (product_id, nome, inv_id, type_id)
                VALUES (:productId, :nome, :inventoryId, :typeId)
                """)
                .param("productId", productId)
                .param("nome", nome)
                .param("inventoryId", inventoryId)
                .param("typeId", typeId)
                .update();
    }

    /**
     * Cria o DataSource H2 usado pelos testes do servico.
     *
     * @return DataSource apontando para um banco H2 em memoria.
     */
    private DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:delete_type_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
