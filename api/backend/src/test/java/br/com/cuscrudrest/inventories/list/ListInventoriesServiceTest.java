package br.com.cuscrudrest.inventories.list;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.inventories.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListInventoriesServiceTest {

    private JdbcClient jdbcClient;
    private ListInventoriesService listInventoriesService;
    private AuthenticatedUserPrincipal authenticatedUser;

    /**
     * Prepara o schema minimo e as dependencias reais do servico de listagem.
     * Entrada: nenhuma.
     * Esperado: servico pronto para listar inventarios com paginacao por offset.
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
        listInventoriesService = new ListInventoriesService(new InventoryRepository(jdbcClient));
    }

    /**
     * Verifica que o servico lista os inventarios acessiveis ordenados por `inv_id`.
     * Entrada: tres inventarios acessiveis ao usuario autenticado com roles distintas.
     * Esperado: pagina com os tres itens ordenados, sem `nextOffset` quando nao houver mais resultados.
     */
    @Test
    void shouldListAccessibleInventoriesOrderedByInventoryId() {
        UUID inventoryId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID inventoryId2 = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
        UUID inventoryId3 = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId3, "Terceiro");
        insertInventory(inventoryId1, "Primeiro");
        insertInventory(inventoryId2, "Segundo");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId3, 2);
        insertInventoryAccess(authenticatedUser.userId(), inventoryId1, 0);
        insertInventoryAccess(authenticatedUser.userId(), inventoryId2, 1);

        ListInventoriesPage page = listInventoriesService.listInventories(authenticatedUser, null, null);

        assertEquals(
                List.of(inventoryId1, inventoryId2, inventoryId3),
                page.inventories().stream().map(ListInventoriesItemResponse::inventoryId).toList()
        );
        assertNull(page.nextOffset());
        assertEquals(200, page.limit());
    }

    /**
     * Verifica que o servico calcula a continuidade da paginacao quando ha mais resultados.
     * Entrada: dois inventarios acessiveis e requisicao com `limit = 1`.
     * Esperado: pagina com um item e `nextOffset = 1`.
     */
    @Test
    void shouldReturnNextOffsetWhenThereAreMoreResults() {
        UUID inventoryId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID inventoryId2 = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId1, "Primeiro");
        insertInventory(inventoryId2, "Segundo");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId1, 0);
        insertInventoryAccess(authenticatedUser.userId(), inventoryId2, 2);

        ListInventoriesPage page = listInventoriesService.listInventories(authenticatedUser, 1, 0);

        assertEquals(1, page.inventories().size());
        assertEquals(1, page.nextOffset());
        assertEquals(1, page.limit());
    }

    /**
     * Verifica que o servico aceita lista vazia na pagina inicial.
     * Entrada: usuario sem inventarios acessiveis e parametros padrao.
     * Esperado: pagina vazia sem `nextOffset`.
     */
    @Test
    void shouldReturnEmptyPageWhenUserHasNoInventories() {
        ListInventoriesPage page = listInventoriesService.listInventories(authenticatedUser, null, null);

        assertEquals(List.of(), page.inventories());
        assertNull(page.nextOffset());
    }

    /**
     * Verifica que o servico rejeita `limit` fora do intervalo aceito.
     * Entrada: requisicao com `limit = 201`.
     * Esperado: NotFoundException associada ao campo `limit`.
     */
    @Test
    void shouldRejectLimitOutsideAcceptedRange() {
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> listInventoriesService.listInventories(authenticatedUser, 201, 0)
        );

        assertEquals("limit", exception.getCampo());
        assertEquals("limit must be between 1 and 200", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita `offset` negativo.
     * Entrada: requisicao com `offset = -1`.
     * Esperado: NotFoundException associada ao campo `offset`.
     */
    @Test
    void shouldRejectNegativeOffset() {
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> listInventoriesService.listInventories(authenticatedUser, 10, -1)
        );

        assertEquals("offset", exception.getCampo());
        assertEquals("offset must be greater than or equal to 0", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita `offset` apos o fim da lista.
     * Entrada: usuario com um inventario acessivel e requisicao com `offset = 1`.
     * Esperado: NotFoundException associada ao campo `offset`.
     */
    @Test
    void shouldRejectOffsetAfterEndOfList() {
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Primeiro");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 0);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> listInventoriesService.listInventories(authenticatedUser, 10, 1)
        );

        assertEquals("offset", exception.getCampo());
        assertEquals("offset after end of list", exception.getInfo());
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
        dataSource.setUrl("jdbc:h2:mem:list_inventories_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
