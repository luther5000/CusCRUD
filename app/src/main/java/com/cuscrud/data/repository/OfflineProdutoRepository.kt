package com.cuscrud.data.repository

import com.cuscrud.data.local.dao.ProdutoDao
import com.cuscrud.data.local.dao.TipoDao
import com.cuscrud.data.mapper.toDomain
import com.cuscrud.data.mapper.toEntity
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Implementação do [ProdutoRepository] que usa um banco de dados local.
 */
class OfflineProdutoRepository @Inject constructor(
    private val produtoDao: ProdutoDao,
    private val tipoDao: TipoDao
) : ProdutoRepository {

    override fun getAllProdutos(): Flow<List<Produto>> =
        combine(produtoDao.getAllFlow(), tipoDao.getAllFlow()) { entities, types ->
            val typesMap = types.associateBy { it.id }
            entities.mapNotNull { entity ->
                typesMap[entity.tipo]?.let { entity.toDomain(it) }
            }
        }

    override suspend fun insertProduto(produto: Produto) {
        // Garante que o ID seja 0 para o Room auto-gerar, caso venha um valor inválido
        val entityToInsert = if (produto.id <= 0) {
            produto.toEntity().copy(id = 0)
        } else {
            produto.toEntity()
        }
        produtoDao.insert(entityToInsert)
    }

    override suspend fun removeProduto(id: Int): Produto? {
        val entity = produtoDao.getById(id) ?: return null
        val types = tipoDao.getAll()
        val typeEntity = types.find { it.id == entity.tipo } ?: return null
        val removedProduto = entity.toDomain(typeEntity)
        produtoDao.delete(entity)
        return removedProduto
    }

    override fun getProdutosByTipo(tipoId: Long): Flow<List<Produto>> =
        combine(produtoDao.getByTipo(tipoId), tipoDao.getAllFlow()) { entities, types ->
            val typeEntity = types.find { it.id == tipoId }
            if (typeEntity == null) {
                emptyList()
            } else {
                entities.map { it.toDomain(typeEntity) }
            }
        }

    override suspend fun editProduto(id: Int, produto: Produto): Produto? {
        val existingEntity = produtoDao.getById(id) ?: return null

        // Busca o TipoEntity diretamente pelo ID do tipo do produto que está sendo editado
        val typeEntity = tipoDao.getById(produto.tipo.id) ?: return null

        // Cria a entidade atualizada usando os dados do produto de domínio recebido,
        // mas mantendo o ID original do produto existente no banco de dados.
        val updatedEntity = produto.toEntity().copy(id = existingEntity.id)

        produtoDao.update(updatedEntity)
        
        // Retorna o produto atualizado, convertendo-o da entidade usando o typeEntity verificado.
        return updatedEntity.toDomain(typeEntity)
    }

    override suspend fun getProdutoById(id: Int): Produto? {
        val entity = produtoDao.getById(id) ?: return null
        val typeEntity = tipoDao.getById(entity.tipo) ?: return null
        return entity.toDomain(typeEntity)
    }
}
