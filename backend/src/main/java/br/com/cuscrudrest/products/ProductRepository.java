package br.com.cuscrudrest.products;

import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JDBC dos produtos.
 * Executa leituras e escritas na tabela `products` associadas a um inventario.
 * Efeitos colaterais: consulta, cria e atualiza registros persistidos no banco.
 */
@Repository
@Conditional(DatabaseConfiguredCondition.class)
public class ProductRepository {

    private final JdbcClient jdbcClient;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Cria o repositorio de produtos.
     *
     * @param jdbcClient facade JDBC configurada para o banco da aplicacao.
     * @param namedParameterJdbcTemplate template JDBC com suporte a parametros nomeados e generated keys.
     */
    public ProductRepository(
            JdbcClient jdbcClient,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate
    ) {
        this.jdbcClient = jdbcClient;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
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

    /**
     * Cria um novo produto associado ao inventario informado.
     * Estrategia: executa `INSERT ... RETURNING` para recuperar imediatamente o estado persistido com `product_id` gerado.
     * Efeitos colaterais: persiste um novo registro na tabela `products`.
     *
     * @param inventoryId identificador do inventario alvo.
     * @param typeId identificador do tipo ao qual o produto pertence.
     * @param marca marca ou fabricante, quando houver.
     * @param dataValidade data de validade com timezone, quando houver.
     * @param unidade unidade base do produto, quando houver.
     * @param unidadeMedida texto da unidade de medida, quando houver.
     * @param quantidade quantidade inicial do produto.
     * @return produto criado com os dados persistidos e `product_id` gerado.
     */
    public ProductSummary createProduct(
            UUID inventoryId,
            long typeId,
            String marca,
            OffsetDateTime dataValidade,
            Long unidade,
            String unidadeMedida,
            long quantidade
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("inventoryId", inventoryId)
                .addValue("typeId", typeId)
                .addValue("marca", marca)
                .addValue("dataValidade", dataValidade)
                .addValue("unidade", unidade)
                .addValue("unidadeMedida", unidadeMedida)
                .addValue("quantidade", quantidade);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        namedParameterJdbcTemplate.update("""
                INSERT INTO products (
                    type_id, marca, dataValidade, unidade, unidadeMedida, quantidade, inv_id
                ) VALUES (
                    :typeId, :marca, :dataValidade, :unidade, :unidadeMedida, :quantidade, :inventoryId
                )
                """, parameters, keyHolder, new String[]{"product_id"});

        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("Produto criado mas nenhuma chave foi retornada.");
        }

        return findProductById(inventoryId, generatedId.longValue())
                .orElseThrow(() -> new IllegalStateException(
                        "Produto criado mas nao encontrado ao reler o registro."
                ));
    }

    /**
     * Atualiza os dados de um produto existente no inventario informado.
     * Estrategia: executa update por `inv_id` e `product_id` e relocaliza o registro para retornar o estado persistido.
     * Efeitos colaterais: persiste novas colunas em um registro da tabela `products`.
     *
     * @param inventoryId identificador do inventario alvo.
     * @param productId identificador do produto a ser atualizado.
     * @param typeId tipo persistido do produto apos a atualizacao.
     * @param marca marca persistida, quando houver.
     * @param dataValidade data de validade persistida, quando houver.
     * @param unidade unidade base persistida, quando houver.
     * @param unidadeMedida texto da unidade de medida persistido, quando houver.
     * @param quantidade quantidade persistida do produto.
     * @return produto atualizado com o estado persistido.
     * @throws IllegalStateException quando o registro e atualizado, mas nao pode ser relido da base.
     */
    public ProductSummary updateProduct(
            UUID inventoryId,
            long productId,
            long typeId,
            String marca,
            OffsetDateTime dataValidade,
            Long unidade,
            String unidadeMedida,
            long quantidade
    ) {
        jdbcClient.sql("""
                UPDATE products
                SET type_id = :typeId,
                    marca = :marca,
                    dataValidade = :dataValidade,
                    unidade = :unidade,
                    unidadeMedida = :unidadeMedida,
                    quantidade = :quantidade
                WHERE inv_id = :inventoryId
                  AND product_id = :productId
                """)
                .param("inventoryId", inventoryId)
                .param("productId", productId)
                .param("typeId", typeId)
                .param("marca", marca)
                .param("dataValidade", dataValidade)
                .param("unidade", unidade)
                .param("unidadeMedida", unidadeMedida)
                .param("quantidade", quantidade)
                .update();

        return findProductById(inventoryId, productId)
                .orElseThrow(() -> new IllegalStateException(
                        "Produto atualizado mas nao encontrado ao reler o registro."
                ));
    }

    /**
     * Remove um produto existente do inventario informado.
     * Estrategia: executa delete direto em `products` por `inv_id` e `product_id`.
     * Efeitos colaterais: remove um registro persistido da tabela `products`.
     *
     * @param inventoryId identificador do inventario alvo.
     * @param productId identificador do produto a ser removido.
     * @return quantidade de linhas removidas.
     */
    public int deleteProduct(UUID inventoryId, long productId) {
        return jdbcClient.sql("""
                DELETE FROM products
                WHERE inv_id = :inventoryId
                  AND product_id = :productId
                """)
                .param("inventoryId", inventoryId)
                .param("productId", productId)
                .update();
    }
}
