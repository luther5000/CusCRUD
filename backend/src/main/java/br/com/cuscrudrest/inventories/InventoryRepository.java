package br.com.cuscrudrest.inventories;

import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

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
