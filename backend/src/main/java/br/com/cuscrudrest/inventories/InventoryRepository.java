package br.com.cuscrudrest.inventories;

import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

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
}
