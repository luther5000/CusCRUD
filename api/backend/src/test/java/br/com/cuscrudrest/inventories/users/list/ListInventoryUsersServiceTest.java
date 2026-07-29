package br.com.cuscrudrest.inventories.users.list;

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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListInventoryUsersServiceTest {

    private JdbcClient jdbcClient;
    private ListInventoryUsersService listInventoryUsersService;
    private AuthenticatedUserPrincipal authenticatedUser;

    /**
     * Prepara o schema minimo e as dependencias reais do servico de listagem de usuarios do inventario.
     * Entrada: nenhuma.
     * Esperado: servico pronto para listar usuarios de inventarios de owner autenticado.
     */
    @BeforeEach
    void setUp() {
        DataSource dataSource = createDataSource();
        jdbcClient = JdbcClient.create(dataSource);
        jdbcClient.sql("DROP TABLE IF EXISTS inventory_access").update();
        jdbcClient.sql("DROP TABLE IF EXISTS inventories").update();
        jdbcClient.sql("DROP TABLE IF EXISTS users").update();
        jdbcClient.sql("""
                CREATE TABLE users (
                    user_id UUID PRIMARY KEY,
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

        UUID ownerUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        insertUser(ownerUserId, "Joao Silva", "joao@example.com");
        authenticatedUser = new AuthenticatedUserPrincipal(
                ownerUserId,
                "Joao Silva",
                "joao@example.com",
                OffsetDateTime.parse("2026-03-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-03-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-03-25T11:00:00-03:00"),
                3600
        );
        InventoryRepository inventoryRepository = new InventoryRepository(jdbcClient);
        listInventoryUsersService = new ListInventoryUsersService(
                new InventoryAccessService(inventoryRepository),
                inventoryRepository
        );
    }

    /**
     * Verifica que o servico lista os usuarios do inventario ordenados por `user_id`.
     * Entrada: inventario com owner, editor e reader persistidos.
     * Esperado: pagina com os tres usuarios ordenados e sem `nextOffset` no fim da lista.
     */
    @Test
    void shouldListInventoryUsersOrderedByUserId() {
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        UUID editorId = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
        UUID readerId = UUID.fromString("770e8400-e29b-41d4-a716-446655440000");
        insertUser(editorId, "Maria Editor", "maria.editor@example.com");
        insertUser(readerId, "Carlos Reader", "carlos.reader@example.com");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 0);
        insertInventoryAccess(editorId, inventoryId, 1);
        insertInventoryAccess(readerId, inventoryId, 2);

        ListInventoryUsersPage page = listInventoryUsersService.listInventoryUsers(
                authenticatedUser,
                inventoryId,
                null,
                null
        );

        assertEquals(inventoryId, page.inventory().inventoryId());
        assertEquals("Estoque da Loja", page.inventory().inventoryName());
        assertEquals(
                List.of(authenticatedUser.userId(), editorId, readerId),
                page.users().stream().map(ListInventoryUsersItemResponse::userId).toList()
        );
        assertNull(page.nextOffset());
    }

    /**
     * Verifica que o servico calcula a continuidade da paginacao quando ha mais usuarios.
     * Entrada: inventario com dois usuarios e requisicao com `limit = 1`.
     * Esperado: pagina com um item e `nextOffset = 1`.
     */
    @Test
    void shouldReturnNextOffsetWhenThereAreMoreUsers() {
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID editorId = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertUser(editorId, "Maria Editor", "maria.editor@example.com");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 0);
        insertInventoryAccess(editorId, inventoryId, 1);

        ListInventoryUsersPage page = listInventoryUsersService.listInventoryUsers(
                authenticatedUser,
                inventoryId,
                1,
                0
        );

        assertEquals(1, page.users().size());
        assertEquals(1, page.nextOffset());
        assertEquals(1, page.limit());
    }

    /**
     * Verifica que o servico rejeita owner ausente do inventario.
     * Entrada: inventario existente sem vinculo do usuario autenticado.
     * Esperado: NotFoundException associada ao campo `inv_id`.
     */
    @Test
    void shouldRejectWhenAuthenticatedUserDoesNotBelongToInventory() {
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> listInventoryUsersService.listInventoryUsers(authenticatedUser, inventoryId, null, null)
        );

        assertEquals("inv_id", exception.getCampo());
        assertEquals("inventory not found", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita usuario autenticado sem role owner.
     * Entrada: inventario existente com vinculo `role = 1` para o usuario autenticado.
     * Esperado: ForbiddenException associada ao campo `inv_id`.
     */
    @Test
    void shouldRejectWhenAuthenticatedUserIsNotOwner() {
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 1);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> listInventoryUsersService.listInventoryUsers(authenticatedUser, inventoryId, null, null)
        );

        assertEquals("inv_id", exception.getCampo());
        assertEquals("owner role required", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita `offset` apos o fim da lista.
     * Entrada: inventario com um unico usuario e requisicao com `offset = 1`.
     * Esperado: NotFoundException associada ao campo `offset`.
     */
    @Test
    void shouldRejectOffsetAfterEndOfList() {
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 0);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> listInventoryUsersService.listInventoryUsers(authenticatedUser, inventoryId, 10, 1)
        );

        assertEquals("offset", exception.getCampo());
        assertEquals("offset after end of list", exception.getInfo());
    }

    /**
     * Persiste um usuario de teste.
     *
     * @param userId identificador do usuario.
     * @param name nome do usuario.
     * @param login email do usuario.
     */
    private void insertUser(UUID userId, String name, String login) {
        jdbcClient.sql("""
                INSERT INTO users (user_id, name, login, passwd)
                VALUES (:userId, :name, :login, :passwd)
                """)
                .param("userId", userId)
                .param("name", name)
                .param("login", login)
                .param("passwd", "$2a$10$abcdefghijklmnopqrstuv")
                .update();
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
        dataSource.setUrl("jdbc:h2:mem:list_inventory_users_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
