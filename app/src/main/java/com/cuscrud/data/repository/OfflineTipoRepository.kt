package com.cuscrud.data.repository

import com.cuscrud.data.local.dao.TipoDao
import com.cuscrud.data.mapper.toDomain
import com.cuscrud.data.mapper.toEntity
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.TipoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementação de um [TipoRepository] que usa um banco de dados local.
 */
class OfflineTipoRepository @Inject constructor(
    private val tipoDao: TipoDao
) : TipoRepository {

    override fun getAllTipos(): Flow<List<Tipo>> =
        tipoDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertTipo(tipo: Tipo) {
        tipoDao.insert(tipo.toEntity())
    }

    override suspend fun removeTipo(id: Long): Tipo? {
        val entity = tipoDao.getById(id) ?: return null
        val domain = entity.toDomain()
        tipoDao.delete(entity)
        return domain
    }

    override suspend fun editTipo(id: Long, tipo: Tipo): Tipo? {
        val existingEntity = tipoDao.getById(id) ?: return null
        
        // Atualiza os campos se eles não estiverem vazios/padrão
        val updatedEntity = existingEntity.copy(
            nome = tipo.nome.ifEmpty { existingEntity.nome },
            imagem = if (tipo.imagem.isNotEmpty()) tipo.imagem else existingEntity.imagem
        )
        
        tipoDao.update(updatedEntity)
        return updatedEntity.toDomain()
    }
}
