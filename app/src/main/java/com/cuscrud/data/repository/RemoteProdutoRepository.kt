package com.cuscrud.data.repository

import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.mapper.toDomain
import com.cuscrud.data.mapper.toRequestDto
import com.cuscrud.data.mapper.toUpdateDto
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.repository.ProdutoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteProdutoRepository @Inject constructor(
    private val apiService: CuscrudApiService,
    private val inventoryRepository: InventoryRepository
) : ProdutoRepository {

    private fun getActiveInvIdOrNull(): String? {
        return inventoryRepository.activeInventoryId.value
    }

    override fun getAllProdutos(): Flow<List<Produto>> = flow {
        try {
            val invId = getActiveInvIdOrNull() ?: return@flow emit(emptyList())
            val response = apiService.getProducts(invId)
            if (response.isSuccessful) {
                emit(response.body()?.map { it.toDomain() } ?: emptyList())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody())
                Timber.e("Erro API (Code: ${response.code()}): $errorMsg")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "Falha de rede em getAllProdutos")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun insertProduto(produto: Produto) {
        try {
            val invId = getActiveInvIdOrNull() ?: return
            val response = apiService.addProduct(invId, produto.toRequestDto())
            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.errorBody())
                Timber.e("Falha ao inserir produto: $errorMsg")
            }
        } catch (e: Exception) {
            Timber.e(e, "Exceção ao inserir produto")
        }
    }

    override suspend fun removeProduto(id: Int): Produto? {
        return try {
            val invId = getActiveInvIdOrNull() ?: return null
            val response = apiService.deleteProduct(invId, id)
            if (response.isSuccessful) null
            else {
                Timber.e("Erro ao remover: ${parseErrorMessage(response.errorBody())}")
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getProdutosByTipo(tipoId: Long): Flow<List<Produto>> = flow {
        try {
            val invId = getActiveInvIdOrNull() ?: return@flow emit(emptyList())
            val response = apiService.getProducts(invId)
            if (response.isSuccessful) {
                val filtered = response.body()?.map { it.toDomain() }
                    ?.filter { it.tipo.id == tipoId } ?: emptyList()
                emit(filtered)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun editProduto(id: Int, produto: Produto): Produto? {
        return try {
            val invId = getActiveInvIdOrNull() ?: return null
            val response = apiService.updateProduct(invId, id, produto.toUpdateDto())
            if (response.isSuccessful) response.body()?.toDomain()
            else {
                Timber.e("Erro ao editar: ${parseErrorMessage(response.errorBody())}")
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getProdutoById(id: Int): Produto? {
        return try {
            val invId = getActiveInvIdOrNull() ?: return null
            val response = apiService.getProducts(invId)
            if (response.isSuccessful) {
                response.body()?.find { it.id == id }?.toDomain()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseErrorMessage(errorBody: ResponseBody?): String {
        return try {
            val jsonString = errorBody?.string() ?: return "Erro desconhecido"
            val jsonElement = Json.parseToJsonElement(jsonString)
            jsonElement.jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content 
                ?: "Erro sem mensagem"
        } catch (e: Exception) {
            "Falha ao processar erro da API"
        }
    }
}
