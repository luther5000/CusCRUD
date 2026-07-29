package br.com.cuscrudrest.inventories.users.delete;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.auth.user.UserRepository;
import br.com.cuscrudrest.common.error.ConflictException;
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

class DeleteInventoryUserServiceTest {

    private JdbcClient jdbcClient;
    private DeleteInventoryUserService deleteInventoryUserService;
    private AuthenticatedUserPrincipal authenticatedUser;

    /**
     * Prepara o schema minimo e as dependencias reais do servico de remocao de usuarios do inventario.
     * Entrada: nenhuma.
     * Esperado: servico pronto para revogar acessos a partir de um owner autenticado.
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
        deleteInventoryUserService = new DeleteInventoryUserService(
                new InventoryAccessService(inventoryRepository),
                inventoryRepository,
                new UserRepository(jdbcClient)
        );
    }

    /**
     * Verifica que o servico remove o acesso de um usuario vinculado ao inventario.
     * Entrada: inventario com owner e usuario alvo com role `1`.
     * Esperado: registro correspondente removido de `inventory_access`.
     */
    @Test
    void shouldDeleteInventoryUserWhenAuthenticatedUserIsOwner() {
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID targetUserId = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 0);
        insertUser(targetUserId, "Maria Editor", "maria.editor@example.com");
        insertInventoryAccess(targetUserId, inventoryId, 1);

        deleteInventoryUserService.deleteInventoryUser(authenticatedUser, inventoryId, targetUserId);

        assertEquals(0, countInventoryAccess(targetUserId, inventoryId));
    }

    /**
     * Verifica que o servico rejeita solicitante sem role owner no inventario.
     * Entrada: usuario autenticado com role `1` no inventario.
     * Esperado: ForbiddenException associada ao campo `inv_id`.
     */
    @Test
    void shouldRejectWhenAuthenticatedUserIsNotOwner() {
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID targetUserId = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 1);
        insertUser(targetUserId, "Maria Editor", "maria.editor@example.com");
        insertInventoryAccess(targetUserId, inventoryId, 2);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> deleteInventoryUserService.deleteInventoryUser(authenticatedUser, inventoryId, targetUserId)
        );

        assertEquals("inv_id", exception.getCampo());
        assertEquals("owner role required", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita usuario inexistente na base.
     * Entrada: `user_id` ausente da tabela `users`.
     * Esperado: NotFoundException associada ao campo `user_id`.
     */
    @Test
    void shouldRejectWhenTargetUserDoesNotExist() {
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID targetUserId = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 0);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> deleteInventoryUserService.deleteInventoryUser(authenticatedUser, inventoryId, targetUserId)
        );

        assertEquals("user_id", exception.getCampo());
        assertEquals("user not found", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita usuario sem vinculo com o inventario.
     * Entrada: usuario existente, mas sem registro correspondente em `inventory_access`.
     * Esperado: NotFoundException associada ao campo `user_id`.
     */
    @Test
    void shouldRejectWhenTargetUserDoesNotBelongToInventory() {
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID targetUserId = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 0);
        insertUser(targetUserId, "Maria Editor", "maria.editor@example.com");

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> deleteInventoryUserService.deleteInventoryUser(authenticatedUser, inventoryId, targetUserId)
        );

        assertEquals("user_id", exception.getCampo());
        assertEquals("user does not have access to inventory", exception.getInfo());
    }

    /**
     * Verifica que o servico rejeita a auto-remocao do owner autenticado.
     * Entrada: owner autenticado como alvo da remocao.
     * Esperado: ConflictException associada ao campo `user_id`.
     */
    @Test
    void shouldRejectWhenOwnerAttemptsToRemoveOwnAccess() {
        UUID inventoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        insertInventory(inventoryId, "Estoque da Loja");
        insertInventoryAccess(authenticatedUser.userId(), inventoryId, 0);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> deleteInventoryUserService.deleteInventoryUser(
                        authenticatedUser,
                        inventoryId,
                        authenticatedUser.userId()
                )
        );

        assertEquals("user_id", exception.getCampo());
        assertEquals("owner cannot remove own access", exception.getInfo());
    }

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

    private int countInventoryAccess(UUID userId, UUID inventoryId) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM inventory_access
                WHERE user_id = :userId AND inv_id = :inventoryId
                """)
                .param("userId", userId)
                .param("inventoryId", inventoryId)
                .query(Integer.class)
                .single();
        return count != null ? count : 0;
    }

    private DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:delete_inventory_user_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("test");
        return dataSource;
    }
}
