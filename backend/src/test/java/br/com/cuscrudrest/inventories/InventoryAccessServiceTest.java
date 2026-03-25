package br.com.cuscrudrest.inventories;

import br.com.cuscrudrest.common.error.ForbiddenException;
import br.com.cuscrudrest.common.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryAccessServiceTest {

    private JdbcClient jdbcClient;
    private InventoryAccessService inventoryAccessService;

    /**
     * Prepara o schema minimo e as dependencias reais do servico de acesso a inventarios.
     * Entrada: nenhuma.
     * Esperado: servico pronto para validar ownership e acesso ao inventario.
     */
    @BeforeEach
    void setUp() {
        DataSource dataSource = createDataSource();
        jdbcClient = JdbcClient.create(dataSource);
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

        inventoryAccessService = new InventoryAccessService(new InventoryRepository(jdbcClient));
    }

    /**
     * Verifica que o servico retorna o contexto quando o usuario e owner do inventario.
     * Entrada: inventario existente com vinculo `role = 0` para o usuario.
     * Esperado: contexto com `inv_id`, `inv_name` e `role = 0`.
     */
    @Test
    void shouldReturnInventoryContextWhenUserIsOwner() {
        UUID inventoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(userId, inventoryId, 0);

        InventoryAccessContext accessContext = inventoryAccessService.requireOwnerAccess(inventoryId, userId);

        assertEquals(inventoryId, accessContext.inventoryId());
        assertEquals("Estoque da Loja", accessContext.inventoryName());
        assertEquals(0, accessContext.role());
    }

    /**
     * Verifica que o servico rejeita inventario inexistente.
     * Entrada: `inv_id` sem registro na tabela `inventories`.
     * Esperado: NotFoundException associada ao campo `inv_id`.
     */
    @Test
    void shouldRejectWhenInventoryDoesNotExist() {
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> inventoryAccessService.requireOwnerAccess(UUID.randomUUID(), UUID.randomUUID())
        );

        assertEquals("inv_id", exception.getCampo());
        assertEquals("inventory not found", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita usuario sem vinculo ao inventario.
     * Entrada: inventario existente sem registro correspondente em `inventory_access`.
     * Esperado: NotFoundException associada ao campo `inv_id`.
     */
    @Test
    void shouldRejectWhenUserDoesNotBelongToInventory() {
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque da Loja");

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> inventoryAccessService.requireOwnerAccess(inventoryId, UUID.randomUUID())
        );

        assertEquals("inv_id", exception.getCampo());
        assertEquals("inventory not found for user", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita usuario autenticado sem role owner.
     * Entrada: inventario existente com vinculo `role = 1` para o usuario.
     * Esperado: ForbiddenException associada ao campo `inv_id`.
     */
    @Test
    void shouldRejectWhenUserIsNotOwner() {
        UUID inventoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(userId, inventoryId, 1);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> inventoryAccessService.requireOwnerAccess(inventoryId, userId)
        );

        assertEquals("inv_id", exception.getCampo());
        assertEquals("owner role required", exception.getInfo());
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
     * Cria o DataSource H2 usado pelos testes do servico.
     *
     * @return DataSource apontando para um banco H2 em memoria.
     */
    private DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:inventory_access_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
