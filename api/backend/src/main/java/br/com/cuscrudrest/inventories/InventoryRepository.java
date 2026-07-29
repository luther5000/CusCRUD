package br.com.cuscrudrest.inventories;

import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JDBC dos inventarios e de seus vinculos de acesso.
 * Executa leituras e escritas nas tabelas `inventories` e `inventory_access` usando SQL explicito.
 * Efeitos colaterais: cria registros persistidos de inventarios e acessos na base.
 */
@Repository
@Conditional(DatabaseConfiguredCondition.class)
public class InventoryRepository {

    private final JdbcClient jdbcClient;

    /**
     * Cria o repositorio de inventarios.
     *
     * @param jdbcClient facade JDBC configurada para o banco da aplicacao.
     */
    public InventoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Conta quantos inventarios o usuario informado possui como owner.
     * Estrategia: consulta a tabela `inventory_access` filtrando por `user_id` e `role = 0`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param userId identificador do usuario autenticado.
     * @return quantidade de inventarios nos quais o usuario e owner.
     */
    public int countOwnedInventories(UUID userId) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM inventory_access
                WHERE user_id = :userId AND role = 0
                """)
                .param("userId", userId)
                .query(Integer.class)
                .single();
        return count != null ? count : 0;
    }

    /**
     * Conta quantos inventarios o usuario informado pode acessar.
     * Estrategia: consulta `inventory_access` filtrando por `user_id`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param userId identificador do usuario autenticado.
     * @return quantidade total de inventarios acessiveis pelo usuario.
     */
    public int countAccessibleInventories(UUID userId) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM inventory_access
                WHERE user_id = :userId
                """)
                .param("userId", userId)
                .query(Integer.class)
                .single();
        return count != null ? count : 0;
    }

    /**
     * Lista os inventarios acessiveis ao usuario com paginacao por offset.
     * Estrategia: faz join entre `inventory_access` e `inventories`, ordenando por `inv_id ASC`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param userId identificador do usuario autenticado.
     * @param limit limite da pagina.
     * @param offset deslocamento da pagina.
     * @return inventarios acessiveis ao usuario na pagina solicitada.
     */
    public List<UserInventorySummary> listAccessibleInventories(UUID userId, int limit, int offset) {
        return jdbcClient.sql("""
                SELECT i.inv_id, i.inv_name, ia.role
                FROM inventory_access ia
                INNER JOIN inventories i ON i.inv_id = ia.inv_id
                WHERE ia.user_id = :userId
                ORDER BY i.inv_id ASC
                LIMIT :limit OFFSET :offset
                """)
                .param("userId", userId)
                .param("limit", limit)
                .param("offset", offset)
                .query((resultSet, rowNum) -> new UserInventorySummary(
                        resultSet.getObject("inv_id", UUID.class),
                        resultSet.getString("inv_name"),
                        resultSet.getInt("role")
                ))
                .list();
    }

    /**
     * Conta quantos usuarios possuem acesso ao inventario informado.
     * Estrategia: consulta `inventory_access` filtrando por `inv_id`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario consultado.
     * @return quantidade total de usuarios com acesso ao inventario.
     */
    public int countInventoryUsers(UUID inventoryId) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM inventory_access
                WHERE inv_id = :inventoryId
                """)
                .param("inventoryId", inventoryId)
                .query(Integer.class)
                .single();
        return count != null ? count : 0;
    }

    /**
     * Lista os usuarios com acesso ao inventario com paginacao por offset.
     * Estrategia: faz join entre `inventory_access` e `users`, ordenando por `user_id ASC`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario consultado.
     * @param limit limite da pagina.
     * @param offset deslocamento da pagina.
     * @return usuarios com acesso ao inventario na pagina solicitada.
     */
    public List<InventoryUserSummary> listInventoryUsers(UUID inventoryId, int limit, int offset) {
        return jdbcClient.sql("""
                SELECT u.user_id, u.name, u.login, ia.role
                FROM inventory_access ia
                INNER JOIN users u ON u.user_id = ia.user_id
                WHERE ia.inv_id = :inventoryId
                ORDER BY u.user_id ASC
                LIMIT :limit OFFSET :offset
                """)
                .param("inventoryId", inventoryId)
                .param("limit", limit)
                .param("offset", offset)
                .query((resultSet, rowNum) -> new InventoryUserSummary(
                        resultSet.getObject("user_id", UUID.class),
                        resultSet.getString("name"),
                        resultSet.getString("login"),
                        resultSet.getInt("role")
                ))
                .list();
    }

    /**
     * Busca um inventario pelo identificador.
     * Estrategia: consulta a tabela `inventories` retornando apenas os campos necessarios para uso interno do dominio.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario.
     * @return inventario encontrado, quando existente.
     */
    public Optional<InventorySummary> findInventoryById(UUID inventoryId) {
        return jdbcClient.sql("""
                SELECT inv_id, inv_name
                FROM inventories
                WHERE inv_id = :inventoryId
                """)
                .param("inventoryId", inventoryId)
                .query((resultSet, rowNum) -> new InventorySummary(
                        resultSet.getObject("inv_id", UUID.class),
                        resultSet.getString("inv_name")
                ))
                .optional();
    }

    /**
     * Busca a role do usuario informado em um inventario.
     * Estrategia: consulta a tabela `inventory_access` por `inv_id` e `user_id`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario.
     * @param userId identificador do usuario autenticado.
     * @return role encontrada para o usuario no inventario, quando existente.
     */
    public Optional<Integer> findUserRole(UUID inventoryId, UUID userId) {
        return jdbcClient.sql("""
                SELECT role
                FROM inventory_access
                WHERE inv_id = :inventoryId AND user_id = :userId
                """)
                .param("inventoryId", inventoryId)
                .param("userId", userId)
                .query(Integer.class)
                .optional();
    }

    /**
     * Cria um novo vinculo de acesso de usuario em um inventario existente.
     * Estrategia: executa insert direto na tabela `inventory_access` com a role ja validada pela camada de servico.
     * Efeitos colaterais: persiste um novo acesso de usuario ao inventario na base.
     *
     * @param inventoryId identificador do inventario.
     * @param userId identificador do usuario que recebera acesso.
     * @param role role a ser atribuida ao usuario no inventario.
     */
    public void addUserAccess(UUID inventoryId, UUID userId, int role) {
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
     * Atualiza a role de um usuario ja vinculado a um inventario.
     * Estrategia: executa update direto na tabela `inventory_access` filtrando por `inv_id` e `user_id`.
     * Efeitos colaterais: persiste a nova role do usuario no inventario.
     *
     * @param inventoryId identificador do inventario.
     * @param userId identificador do usuario alvo.
     * @param role nova role a ser persistida.
     */
    public void updateUserAccessRole(UUID inventoryId, UUID userId, int role) {
        jdbcClient.sql("""
                UPDATE inventory_access
                SET role = :role
                WHERE inv_id = :inventoryId AND user_id = :userId
                """)
                .param("role", role)
                .param("inventoryId", inventoryId)
                .param("userId", userId)
                .update();
    }

    /**
     * Remove o vinculo de acesso de um usuario a um inventario.
     * Estrategia: executa delete direto na tabela `inventory_access` filtrando por `inv_id` e `user_id`.
     * Efeitos colaterais: remove o acesso persistido do usuario ao inventario.
     *
     * @param inventoryId identificador do inventario.
     * @param userId identificador do usuario alvo.
     */
    public void deleteUserAccess(UUID inventoryId, UUID userId) {
        jdbcClient.sql("""
                DELETE FROM inventory_access
                WHERE inv_id = :inventoryId AND user_id = :userId
                """)
                .param("inventoryId", inventoryId)
                .param("userId", userId)
                .update();
    }

    /**
     * Cria um inventario e o vinculo de owner para o usuario informado.
     * Estrategia: insere o inventario na tabela `inventories` e depois cria o acesso correspondente em `inventory_access`.
     * Efeitos colaterais: cria um inventario persistido e o papel owner na base.
     *
     * @param inventoryId identificador do inventario a ser persistido.
     * @param inventoryName nome do inventario.
     * @param userId identificador do usuario que sera owner do inventario.
     */
    public void createInventoryWithOwner(UUID inventoryId, String inventoryName, UUID userId) {
        jdbcClient.sql("""
                INSERT INTO inventories (inv_id, inv_name)
                VALUES (:inventoryId, :inventoryName)
                """)
                .param("inventoryId", inventoryId)
                .param("inventoryName", inventoryName)
                .update();

        jdbcClient.sql("""
                INSERT INTO inventory_access (user_id, inv_id, role)
                VALUES (:userId, :inventoryId, 0)
                """)
                .param("userId", userId)
                .param("inventoryId", inventoryId)
                .update();
    }

    /**
     * Atualiza o nome de um inventario existente.
     * Estrategia: executa update direto na tabela `inventories` filtrando por `inv_id`.
     * Efeitos colaterais: persiste o novo nome do inventario na base.
     *
     * @param inventoryId identificador do inventario a ser atualizado.
     * @param inventoryName novo nome a ser persistido.
     */
    public void renameInventory(UUID inventoryId, String inventoryName) {
        jdbcClient.sql("""
                UPDATE inventories
                SET inv_name = :inventoryName
                WHERE inv_id = :inventoryId
                """)
                .param("inventoryId", inventoryId)
                .param("inventoryName", inventoryName)
                .update();
    }

    /**
     * Remove um inventario existente.
     * Estrategia: executa delete direto na tabela `inventories` filtrando por `inv_id`.
     * Efeitos colaterais: remove o inventario persistido da base.
     *
     * @param inventoryId identificador do inventario a ser removido.
     */
    public void deleteInventory(UUID inventoryId) {
        jdbcClient.sql("""
                DELETE FROM inventories
                WHERE inv_id = :inventoryId
                """)
                .param("inventoryId", inventoryId)
                .update();
    }
}
