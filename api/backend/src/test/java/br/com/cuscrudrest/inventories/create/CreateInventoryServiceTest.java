package br.com.cuscrudrest.inventories.create;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.inventories.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateInventoryServiceTest {

    private JdbcClient jdbcClient;
    private CreateInventoryService createInventoryService;
    private AuthenticatedUserPrincipal authenticatedUser;

    /**
     * Prepara o schema minimo e as dependencias reais do servico de criacao de inventario.
     * Entrada: nenhuma.
     * Esperado: servico pronto para criar inventarios e conceder ownership.
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

        UUID userId = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO users (user_id, name, login, passwd, created_at)
                VALUES (:userId, :name, :login, :passwd, :createdAt)
                """)
                .param("userId", userId)
                .param("name", "Joao Novo")
                .param("login", "joao.novo@example.com")
                .param("passwd", "$2a$10$abcdefghijklmnopqrstuv")
                .param("createdAt", OffsetDateTime.parse("2026-03-25T10:00:00-03:00"))
                .update();

        authenticatedUser = new AuthenticatedUserPrincipal(
                userId,
                "Joao Novo",
                "joao.novo@example.com",
                OffsetDateTime.parse("2026-03-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-03-25T11:00:00-03:00"),
                OffsetDateTime.parse("2026-03-25T12:00:00-03:00"),
                3600
        );

        createInventoryService = new CreateInventoryService(new InventoryRepository(jdbcClient));
    }

    /**
     * Verifica que o servico cria um inventario valido e concede o papel owner.
     * Entrada: usuario autenticado e nome valido de inventario.
     * Esperado: inventario persistido, resposta com `inv_id`, `inv_name` e `role = 0`.
     */
    @Test
    void shouldCreateInventoryAndGrantOwnerRole() {
        CreateInventoryResponse response = createInventoryService.createInventory(
                authenticatedUser,
                new CreateInventoryRequest("Estoque da Loja")
        );

        assertNotNull(response.inventory().inventoryId());
        assertEquals("Estoque da Loja", response.inventory().inventoryName());
        assertEquals(0, response.role());

        String persistedName = jdbcClient.sql("SELECT inv_name FROM inventories WHERE inv_id = :inventoryId")
                .param("inventoryId", response.inventory().inventoryId())
                .query(String.class)
                .single();

        Integer persistedRole = jdbcClient.sql("""
                SELECT role
                FROM inventory_access
                WHERE user_id = :userId AND inv_id = :inventoryId
                """)
                .param("userId", authenticatedUser.userId())
                .param("inventoryId", response.inventory().inventoryId())
                .query(Integer.class)
                .single();

        assertEquals("Estoque da Loja", persistedName);
        assertEquals(0, persistedRole);
    }

    /**
     * Verifica que o servico rejeita criacao acima do limite de inventarios como owner.
     * Entrada: usuario com 100 inventarios owner ja persistidos.
     * Esperado: ConflictException associada ao campo `inventory`.
     */
    @Test
    void shouldRejectCreationWhenOwnerInventoryLimitIsReached() {
        for (int index = 0; index < 100; index++) {
            UUID inventoryId = UUID.randomUUID();
            jdbcClient.sql("INSERT INTO inventories (inv_id, inv_name) VALUES (:inventoryId, :inventoryName)")
                    .param("inventoryId", inventoryId)
                    .param("inventoryName", "Inventario " + index)
                    .update();
            jdbcClient.sql("""
                    INSERT INTO inventory_access (user_id, inv_id, role)
                    VALUES (:userId, :inventoryId, 0)
                    """)
                    .param("userId", authenticatedUser.userId())
                    .param("inventoryId", inventoryId)
                    .update();
        }

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> createInventoryService.createInventory(authenticatedUser, new CreateInventoryRequest("Estoque da Loja"))
        );

        assertEquals("inventory", exception.getCampo());
        assertEquals("owner inventory limit reached", exception.getInfo());
    }

    /**
     * Cria o DataSource H2 usado pelos testes do servico.
     *
     * @return DataSource apontando para um banco H2 em memoria.
     */
    private DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:create_inventory_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
