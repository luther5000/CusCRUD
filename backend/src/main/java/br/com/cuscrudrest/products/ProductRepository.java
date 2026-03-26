package br.com.cuscrudrest.products;

import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JDBC dos produtos.
 * Executa leituras na tabela `products` associadas a um inventario.
 * Efeitos colaterais: nenhum alem de consultas ao banco.
 */
@Repository
@Conditional(DatabaseConfiguredCondition.class)
public class ProductRepository {

    private final JdbcClient jdbcClient;

    /**
     * Cria o repositorio de produtos.
     *
     * @param jdbcClient facade JDBC configurada para o banco da aplicacao.
     */
    public ProductRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Conta quantos produtos pertencem ao inventario informado.
     * Estrategia: consulta a tabela `products` filtrando por `inv_id`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario consultado.
     * @return quantidade total de produtos do inventario.
     */
    public int countProducts(UUID inventoryId) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM products
                WHERE inv_id = :inventoryId
                """)
                .param("inventoryId", inventoryId)
                .query(Integer.class)
                .single();
        return count != null ? count : 0;
    }

    /**
     * Conta quantos produtos pertencem ao inventario informado para um tipo especifico.
     * Estrategia: consulta a tabela `products` filtrando por `inv_id` e `type_id`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario consultado.
     * @param typeId identificador do tipo pelo qual a listagem sera filtrada.
     * @return quantidade total de produtos do tipo no inventario.
     */
    public int countProductsByType(UUID inventoryId, long typeId) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM products
                WHERE inv_id = :inventoryId AND type_id = :typeId
                """)
                .param("inventoryId", inventoryId)
                .param("typeId", typeId)
                .query(Integer.class)
                .single();
        return count != null ? count : 0;
    }

    /**
     * Lista os produtos do inventario com paginacao por offset.
     * Estrategia: consulta a tabela `products` ordenando por `product_id ASC`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario consultado.
     * @param limit limite da pagina.
     * @param offset deslocamento da pagina.
     * @return produtos da pagina solicitada.
     */
    public List<ProductSummary> listProducts(UUID inventoryId, int limit, int offset) {
        return jdbcClient.sql("""
                SELECT product_id, type_id, marca, dataValidade, unidade, unidadeMedida, quantidade, inv_id
                FROM products
                WHERE inv_id = :inventoryId
                ORDER BY product_id ASC
                LIMIT :limit OFFSET :offset
                """)
                .param("inventoryId", inventoryId)
                .param("limit", limit)
                .param("offset", offset)
                .query((resultSet, rowNum) -> new ProductSummary(
                        resultSet.getLong("product_id"),
                        resultSet.getLong("type_id"),
                        resultSet.getString("marca"),
                        resultSet.getObject("dataValidade", java.time.OffsetDateTime.class),
                        resultSet.getObject("unidade", Long.class),
                        resultSet.getString("unidadeMedida"),
                        resultSet.getLong("quantidade"),
                        resultSet.getObject("inv_id", UUID.class)
                ))
                .list();
    }

    /**
     * Lista os produtos do inventario filtrados por tipo com paginacao por offset.
     * Estrategia: consulta a tabela `products` filtrando por `inv_id` e `type_id`, ordenando por `product_id ASC`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario consultado.
     * @param typeId identificador do tipo pelo qual a listagem sera filtrada.
     * @param limit limite da pagina.
     * @param offset deslocamento da pagina.
     * @return produtos da pagina solicitada para o tipo informado.
     */
    public List<ProductSummary> listProductsByType(UUID inventoryId, long typeId, int limit, int offset) {
        return jdbcClient.sql("""
                SELECT product_id, type_id, marca, dataValidade, unidade, unidadeMedida, quantidade, inv_id
                FROM products
                WHERE inv_id = :inventoryId
                  AND type_id = :typeId
                ORDER BY product_id ASC
                LIMIT :limit OFFSET :offset
                """)
                .param("inventoryId", inventoryId)
                .param("typeId", typeId)
                .param("limit", limit)
                .param("offset", offset)
                .query((resultSet, rowNum) -> new ProductSummary(
                        resultSet.getLong("product_id"),
                        resultSet.getLong("type_id"),
                        resultSet.getString("marca"),
                        resultSet.getObject("dataValidade", java.time.OffsetDateTime.class),
                        resultSet.getObject("unidade", Long.class),
                        resultSet.getString("unidadeMedida"),
                        resultSet.getLong("quantidade"),
                        resultSet.getObject("inv_id", UUID.class)
                ))
                .list();
    }

    /**
     * Busca um produto especifico do inventario informado.
     * Estrategia: consulta a tabela `products` filtrando por `inv_id` e `product_id`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario consultado.
     * @param productId identificador do produto a ser localizado.
     * @return produto encontrado, quando existir para o inventario informado.
     */
    public Optional<ProductSummary> findProductById(UUID inventoryId, long productId) {
        return jdbcClient.sql("""
                SELECT product_id, type_id, marca, dataValidade, unidade, unidadeMedida, quantidade, inv_id
                FROM products
                WHERE inv_id = :inventoryId AND product_id = :productId
                """)
                .param("inventoryId", inventoryId)
                .param("productId", productId)
                .query((resultSet, rowNum) -> new ProductSummary(
                        resultSet.getLong("product_id"),
                        resultSet.getLong("type_id"),
                        resultSet.getString("marca"),
                        resultSet.getObject("dataValidade", java.time.OffsetDateTime.class),
                        resultSet.getObject("unidade", Long.class),
                        resultSet.getString("unidadeMedida"),
                        resultSet.getLong("quantidade"),
                        resultSet.getObject("inv_id", UUID.class)
                ))
                .optional();
    }
}
