package com.cuscrud.data.repository

import com.cuscrud.data.local.dao.ProdutoDao
import com.cuscrud.data.local.dao.TipoDao
import com.cuscrud.data.mapper.toDomain
import com.cuscrud.data.mapper.toEntity
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementação do [ProdutoRepository] que usa um banco de dados local (Room).
 * Atualizada para respeitar a nova interface que retorna [Result] e utiliza chamadas suspensas.
 * 
 * NOTA: Esta classe está sendo mantida apenas para compatibilidade de build, 
 * já que a fonte de verdade agora é a API Remota.
 */
class OfflineProdutoRepository @Inject constructor(
    private val produtoDao: ProdutoDao,
    private val tipoDao: TipoDao
) : ProdutoRepository {

    override suspend fun getProdutos(limit: Int, offset: Int): Result<List<Produto>> = withContext(Dispatchers.IO) {
        try {
            val entities = produtoDao.getAll()
            val types = tipoDao.getAll().associateBy { it.id }
            val produtos = entities.mapNotNull { entity ->
                types[entity.tipo]?.let { entity.toDomain(it) }
            }
            Result.Success(produtos)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun insertProduto(produto: Produto): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entityToInsert = if (produto.id <= 0L) {
                produto.toEntity().copy(id = 0L)
            } else {
                produto.toEntity()
            }
            produtoDao.insert(entityToInsert)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeProduto(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = produtoDao.getById(id)
            if (entity != null) {
                produtoDao.delete(entity)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Produto não encontrado"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getProdutosByTipo(tipoId: Long): Result<List<Produto>> = withContext(Dispatchers.IO) {
        try {
            val entities = produtoDao.getByTipoSync(tipoId) // Assume-se que existe uma versão sync
            val typeEntity = tipoDao.getById(tipoId)
            if (typeEntity == null) {
                Result.Success(emptyList())
            } else {
                Result.Success(entities.map { it.toDomain(typeEntity) })
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun editProduto(id: Long, produto: Produto): Result<Produto> = withContext(Dispatchers.IO) {
        try {
            val existingEntity = produtoDao.getById(id) ?: return@withContext Result.Error(Exception("Não encontrado"))
            val typeEntity = tipoDao.getById(produto.tipo.id) ?: return@withContext Result.Error(Exception("Tipo não encontrado"))
            val updatedEntity = produto.toEntity().copy(id = existingEntity.id)
            produtoDao.update(updatedEntity)
            Result.Success(updatedEntity.toDomain(typeEntity))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getProdutoById(id: Long): Result<Produto> = withContext(Dispatchers.IO) {
        try {
            val entity = produtoDao.getById(id) ?: return@withContext Result.Error(Exception("Não encontrado"))
            val typeEntity = tipoDao.getById(entity.tipo) ?: return@withContext Result.Error(Exception("Tipo não encontrado"))
            Result.Success(entity.toDomain(typeEntity))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
