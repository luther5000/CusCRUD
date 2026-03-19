package com.cuscrud.data.repository

import com.cuscrud.data.local.dao.ProdutoDao
import com.cuscrud.data.local.dao.TipoDao
import com.cuscrud.data.mapper.toDomain
import com.cuscrud.data.mapper.toEntity
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
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
        produtoDao.insert(produto.toEntity())
    }

    override suspend fun removeProduto(id: Int): Result<Unit> {
        return try {
            // Buscamos apenas para confirmar existência se necessário,
            // ou deletamos direto pelo ID se o DAO permitir.
            val entity = produtoDao.getById(id)
                ?: return Result.Error(Exception("Produto não encontrado"))

            produtoDao.delete(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            // Captura erros de banco de dados (ex: exclusão impedida por chave estrangeira)
            Result.Error(e)
        }
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

        // Update existing fields only if the new fields are not empty/default
        val updatedEntity = existingEntity.copy(
            marca = produto.marca.ifEmpty { existingEntity.marca },
            quantidade = if (produto.quantidade > 0) produto.quantidade else existingEntity.quantidade,
            unidade = if (produto.unidade > 0) produto.unidade else existingEntity.unidade,
            unidadeMedida = produto.unidadeMedida.ifEmpty { existingEntity.unidadeMedida },
            tipo = if (produto.tipo.id > 0L) produto.tipo.id else existingEntity.tipo,
            dataValidade = if (produto.dataValidade.time > 0) produto.dataValidade.time else existingEntity.dataValidade
        )

        produtoDao.update(updatedEntity)
        
        val types = tipoDao.getAll()
        val typeEntity = types.find { it.id == updatedEntity.tipo } ?: return null
        return updatedEntity.toDomain(typeEntity)
    }

    override suspend fun getProdutoById(id: Int): Produto? {
        val entity = produtoDao.getById(id) ?: return null
        val typeEntity = tipoDao.getById(entity.tipo) ?: return null
        return entity.toDomain(typeEntity)
    }
}