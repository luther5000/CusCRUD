package com.cuscrud.data.repository

import com.cuscrud.data.mapper.toDomain
import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.CreateTipoRequest
import com.cuscrud.data.remote.dto.UpdateTipoRequest
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.repository.TipoRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Implementação remota do repositório de Tipos que se comunica com a API REST.
 * 
 * Segue a Seção 5.4 do architecture.md, vinculando todas as operações ao [inv_id] 
 * do inventário ativo obtido via [InventoryRepository].
 */
class RemoteTipoRepository @Inject constructor(
    private val apiService: CuscrudApiService,
    private val inventoryRepository: InventoryRepository
) : TipoRepository {

    /**
     * Recupera o ID do inventário ativo. Se não houver inventário selecionado, retorna null.
     */
    private fun getActiveInventoryId(): String? = inventoryRepository.activeInventoryId.value

    /**
     * Recupera uma lista paginada de tipos para o inventário ativo na nuvem.
     */
    override suspend fun getTipos(limit: Int, offset: Int): Result<List<Tipo>> = withContext(Dispatchers.IO) {
        val invId = getActiveInventoryId() ?: return@withContext Result.Error(Exception("Nenhum inventário ativo selecionado."))
        
        try {
            val response = apiService.getTypes(invId, limit, offset)
            if (response.isSuccessful) {
                val types = response.body()?.types?.map { it.toDomain() } ?: emptyList()
                Result.Success(types)
            } else {
                Result.Error(HttpException(response))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Busca um tipo específico pelo seu identificador na nuvem.
     */
    override suspend fun getTipoById(id: Long): Result<Tipo> = withContext(Dispatchers.IO) {
        val invId = getActiveInventoryId() ?: return@withContext Result.Error(Exception("Nenhum inventário ativo selecionado."))

        try {
            val response = apiService.getTypeById(invId, id)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it.toDomain())
                } ?: Result.Error(Exception("Tipo não encontrado."))
            } else {
                Result.Error(HttpException(response))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Cria um novo tipo no inventário ativo na nuvem.
     */
    override suspend fun insertTipo(nome: String, imagemBase64: String?): Result<Tipo> = withContext(Dispatchers.IO) {
        val invId = getActiveInventoryId() ?: return@withContext Result.Error(Exception("Nenhum inventário ativo selecionado."))

        try {
            val request = CreateTipoRequest(nome = nome, imagem = imagemBase64)
            val response = apiService.createType(invId, request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it.toDomain())
                } ?: Result.Error(Exception("Erro ao criar tipo."))
            } else {
                Result.Error(HttpException(response))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Atualiza os dados de um tipo existente na nuvem.
     */
    override suspend fun editTipo(id: Long, nome: String?, imagemBase64: String?): Result<Tipo> = withContext(Dispatchers.IO) {
        val invId = getActiveInventoryId() ?: return@withContext Result.Error(Exception("Nenhum inventário ativo selecionado."))

        try {
            val request = UpdateTipoRequest(nome = nome, imagem = imagemBase64)
            val response = apiService.updateType(invId, id, request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it.toDomain())
                } ?: Result.Error(Exception("Erro ao atualizar tipo."))
            } else {
                Result.Error(HttpException(response))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Remove um tipo do inventário na nuvem.
     * Retorna erro 409 se houver produtos vinculados.
     */
    override suspend fun removeTipo(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val invId = getActiveInventoryId() ?: return@withContext Result.Error(Exception("Nenhum inventário ativo selecionado."))

        try {
            val response = apiService.deleteType(invId, id)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else if (response.code() == 409) {
                Result.Error(Exception("Não é possível excluir este tipo pois existem produtos vinculados a ele."))
            } else {
                Result.Error(HttpException(response))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
