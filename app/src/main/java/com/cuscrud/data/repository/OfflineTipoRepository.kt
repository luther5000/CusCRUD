package com.cuscrud.data.repository

import com.cuscrud.data.local.dao.TipoDao
import com.cuscrud.data.mapper.toDomain
import com.cuscrud.data.mapper.toEntity
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.TipoRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementação de um [TipoRepository] que usa um banco de dados local (Room).
 * Adaptado para a nova interface assíncrona.
 */
class OfflineTipoRepository @Inject constructor(
    private val tipoDao: TipoDao
) : TipoRepository {

    override suspend fun getTipos(limit: Int, offset: Int): Result<List<Tipo>> = withContext(Dispatchers.IO) {
        try {
            // Nota: O DAO antigo pode não ter suporte a limit/offset, retornando tudo por enquanto.
            val entities = tipoDao.getAll() 
            Result.Success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getTipoById(id: Long): Result<Tipo> = withContext(Dispatchers.IO) {
        try {
            val entity = tipoDao.getById(id)
            if (entity != null) {
                Result.Success(entity.toDomain())
            } else {
                Result.Error(Exception("Tipo não encontrado localmente."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun insertTipo(nome: String, imagemBase64: String?): Result<Tipo> = withContext(Dispatchers.IO) {
        try {
            // Conversão manual para ByteArray para o Room
            val imageBytes = imagemBase64?.let { 
                android.util.Base64.decode(it, android.util.Base64.DEFAULT) 
            } ?: byteArrayOf()
            
            val tipo = Tipo(id = 0, nome = nome, imagem = imageBytes)
            val id = tipoDao.insert(tipo.toEntity())
            Result.Success(tipo.copy(id = id))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeTipo(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = tipoDao.getById(id)
            if (entity != null) {
                tipoDao.delete(entity)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Tipo não encontrado para remoção."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun editTipo(id: Long, nome: String?, imagemBase64: String?): Result<Tipo> = withContext(Dispatchers.IO) {
        try {
            val existingEntity = tipoDao.getById(id) ?: return@withContext Result.Error(Exception("Tipo não encontrado."))
            
            val imageBytes = imagemBase64?.let { 
                android.util.Base64.decode(it, android.util.Base64.DEFAULT) 
            } ?: existingEntity.imagem

            val updatedEntity = existingEntity.copy(
                nome = nome ?: existingEntity.nome,
                imagem = imageBytes
            )
            
            tipoDao.update(updatedEntity)
            Result.Success(updatedEntity.toDomain())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
