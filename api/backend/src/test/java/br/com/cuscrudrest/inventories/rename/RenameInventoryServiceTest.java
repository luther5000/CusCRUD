package br.com.cuscrudrest.inventories.rename;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ForbiddenException;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.inventories.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenameInventoryServiceTest {

    private JdbcClient jdbcClient;
    private RenameInventoryService renameInventoryService;
    private AuthenticatedUserPrincipal authenticatedUser;

    /**
     * Prepara o schema minimo e as dependencias reais do servico de renomeacao.
     * Entrada: nenhuma.
     * Esperado: servico pronto para renomear inventarios de owners autenticados.
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
        InventoryRepository inventoryRepository = new InventoryRepository(jdbcClient);
        renameInventoryService = new RenameInventoryService(
                new InventoryAccessService(inventoryRepository),
                inventoryRepository
        );
    }

    /**
     * Verifica que o servico renomeia o inventario quando o usuario e owner.
     * Entrada: inventario existente com vinculo `role = 0` para o usuario autenticado.
     * Esperado: nome atualizado no banco e resposta com `role = 0`.
     */
    @Test
    void shouldRenameInventoryWhenUserIsOwner() {
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque Antigo");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 0);

        RenameInventoryResponse response = renameInventoryService.renameInventory(
                authenticatedUser,
                inventoryId,
                new RenameInventoryRequest("Estoque Renomeado")
        );

        assertEquals(inventoryId, response.inventory().inventoryId());
        assertEquals("Estoque Renomeado", response.inventory().inventoryName());
        assertEquals(0, response.role());
        assertEquals(
                "Estoque Renomeado",
                jdbcClient.sql("SELECT inv_name FROM inventories WHERE inv_id = :inventoryId")
                        .param("inventoryId", inventoryId)
                        .query(String.class)
                        .single()
        );
    }

    /**
     * Verifica que o servico rejeita inventario inexistente.
     * Entrada: `inv_id` sem registro.
     * Esperado: NotFoundException associada ao campo `inv_id`.
     */
    @Test
    void shouldRejectWhenInventoryDoesNotExist() {
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> renameInventoryService.renameInventory(
                        authenticatedUser,
                        UUID.randomUUID(),
                        new RenameInventoryRequest("Estoque Renomeado")
                )
        );

        assertEquals("inv_id", exception.getCampo());
        assertEquals("inventory not found", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita usuario sem role owner.
     * Entrada: inventario existente com vinculo `role = 1`.
     * Esperado: ForbiddenException associada ao campo `inv_id`.
     */
    @Test
    void shouldRejectWhenUserIsNotOwner() {
        UUID inventoryId = UUID.randomUUID();
        insertInventory(inventoryId, "Estoque Antigo");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 1);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> renameInventoryService.renameInventory(
                        authenticatedUser,
                        inventoryId,
                        new RenameInventoryRequest("Estoque Renomeado")
                )
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
        dataSource.setUrl("jdbc:h2:mem:rename_inventory_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
