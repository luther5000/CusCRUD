package br.com.cuscrudrest.types;

import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JDBC dos tipos de produto.
 * Executa leituras nas tabelas `types` associadas a um inventario.
 * Efeitos colaterais: nenhum alem de consultas ao banco.
 */
@Repository
@Conditional(DatabaseConfiguredCondition.class)
public class TypeRepository {

    private final JdbcClient jdbcClient;

    /**
     * Cria o repositorio de tipos.
     *
     * @param jdbcClient facade JDBC configurada para o banco da aplicacao.
     */
    public TypeRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Conta quantos tipos pertencem ao inventario informado.
     * Estrategia: consulta a tabela `types` filtrando por `inv_id`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario consultado.
     * @return quantidade total de tipos do inventario.
     */
    public int countTypes(UUID inventoryId) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM types
                WHERE inv_id = :inventoryId
                """)
                .param("inventoryId", inventoryId)
                .query(Integer.class)
                .single();
        return count != null ? count : 0;
    }

    /**
     * Lista os tipos do inventario com paginacao por offset.
     * Estrategia: consulta a tabela `types` ordenando por `type_id ASC`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario consultado.
     * @param limit limite da pagina.
     * @param offset deslocamento da pagina.
     * @return tipos da pagina solicitada.
     */
    public List<TypeSummary> listTypes(UUID inventoryId, int limit, int offset) {
        return jdbcClient.sql("""
                SELECT type_id, nome, imagem IS NOT NULL AS has_image
                FROM types
                WHERE inv_id = :inventoryId
                ORDER BY type_id ASC
                LIMIT :limit OFFSET :offset
                """)
                .param("inventoryId", inventoryId)
                .param("limit", limit)
                .param("offset", offset)
                .query((resultSet, rowNum) -> new TypeSummary(
                        resultSet.getLong("type_id"),
                        resultSet.getString("nome"),
                        resultSet.getBoolean("has_image")
                ))
                .list();
    }

    /**
     * Busca um tipo especifico do inventario informado.
     * Estrategia: consulta a tabela `types` filtrando por `inv_id` e `type_id`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario consultado.
     * @param typeId identificador do tipo a ser localizado.
     * @return tipo encontrado, quando existir para o inventario informado.
     */
    public Optional<TypeDetails> findTypeById(UUID inventoryId, long typeId) {
        return jdbcClient.sql("""
                SELECT type_id, nome, imagem, inv_id
                FROM types
                WHERE inv_id = :inventoryId AND type_id = :typeId
                """)
                .param("inventoryId", inventoryId)
                .param("typeId", typeId)
                .query((resultSet, rowNum) -> new TypeDetails(
                        resultSet.getLong("type_id"),
                        resultSet.getString("nome"),
                        resultSet.getBytes("imagem"),
                        resultSet.getObject("inv_id", UUID.class)
                ))
                .optional();
    }

    /**
     * Verifica se ja existe um tipo com o mesmo nome no inventario informado.
     * Estrategia: consulta a tabela `types` filtrando por `inv_id` e `nome`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param inventoryId identificador do inventario alvo.
     * @param nome nome do tipo cuja unicidade sera validada.
     * @return `true` quando ja existir outro tipo com o mesmo nome no inventario.
     */
    public boolean existsTypeByInventoryIdAndName(UUID inventoryId, String nome) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM types
                WHERE inv_id = :inventoryId AND nome = :nome
                """)
                .param("inventoryId", inventoryId)
                .param("nome", nome)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    /**
     * Cria um novo tipo associado ao inventario informado.
     * Estrategia: executa o insert e relocaliza o registro pelo par unico `(inv_id, nome)` para recuperar o `type_id`.
     * Efeitos colaterais: persiste um novo registro na tabela `types`.
     *
     * @param inventoryId identificador do inventario alvo.
     * @param nome nome do tipo a ser criado.
     * @param imagem bytes da imagem associada, quando houver.
     * @return tipo criado com os dados persistidos e `type_id` gerado.
     * @throws IllegalStateException quando o registro e criado, mas nao pode ser relido da base.
     */
    public TypeDetails createType(UUID inventoryId, String nome, byte[] imagem) {
        jdbcClient.sql("""
                INSERT INTO types (nome, imagem, inv_id)
                VALUES (:nome, :imagem, :inventoryId)
                """)
                .param("nome", nome)
                .param("imagem", imagem)
                .param("inventoryId", inventoryId)
                .update();

        return jdbcClient.sql("""
                SELECT type_id, nome, imagem, inv_id
                FROM types
                WHERE inv_id = :inventoryId AND nome = :nome
                """)
                .param("inventoryId", inventoryId)
                .param("nome", nome)
                .query((resultSet, rowNum) -> new TypeDetails(
                        resultSet.getLong("type_id"),
                        resultSet.getString("nome"),
                        resultSet.getBytes("imagem"),
                        resultSet.getObject("inv_id", UUID.class)
                ))
                .optional()
                .orElseThrow(() -> new IllegalStateException(
                        "Tipo criado mas nao encontrado ao reler o registro."
                ));
    }
}
