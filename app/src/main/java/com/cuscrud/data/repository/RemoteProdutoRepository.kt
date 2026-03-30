package com.cuscrud.data.repository

import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.mapper.toDomain
import com.cuscrud.data.mapper.toRequestDto
import com.cuscrud.data.mapper.toUpdateDto
import com.cuscrud.data.remote.dto.ErrorResponse
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do repositório [ProdutoRepository] que gerencia as operações de produtos via API remota.
 * Refatorado para retornar [Result] e utilizar chamadas suspensas one-shot.
 */

@Singleton
class RemoteProdutoRepository @Inject constructor(
    private val apiService: CuscrudApiService,
    private val inventoryRepository: InventoryRepository,
    private val json: Json
) : ProdutoRepository {

    private fun getActiveInvIdOrNull(): String? {
        return inventoryRepository.activeInventoryId.value
    }

    override suspend fun getProdutos(limit: Int, offset: Int): Result<List<Produto>> = withContext(Dispatchers.IO) {
        val invId = getActiveInvIdOrNull() ?: return@withContext Result.Error(Exception("Nenhum inventário ativo selecionado."))
        try {
            val response = apiService.getProducts(invId, limit, offset)
            if (response.isSuccessful) {
                Result.Success(response.body()?.map { it.toDomain() } ?: emptyList())
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Falha de conexão. Verifique sua internet."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar produtos")
            Result.Error(Exception("Não foi possível carregar os produtos."))
        }
    }

    override suspend fun insertProduto(produto: Produto): Result<Unit> = withContext(Dispatchers.IO) {
        val invId = getActiveInvIdOrNull() ?: return@withContext Result.Error(Exception("Identificador de inventário não encontrado."))
        try {
            val response = apiService.addProduct(invId, produto.toRequestDto())
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Não foi possível salvar o produto. Verifique sua conexão."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao inserir produto")
            Result.Error(Exception("Erro ao salvar o produto."))
        }
    }

    override suspend fun removeProduto(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val invId = getActiveInvIdOrNull() ?: return@withContext Result.Error(Exception("Identificador de inventário não encontrado."))
        try {
            val response = apiService.deleteProduct(invId, id)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Erro ao excluir produto. Verifique sua conexão."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao remover produto")
            Result.Error(Exception("Erro ao excluir produto."))
        }
    }

    override suspend fun getProdutosByTipo(tipoId: Long): Result<List<Produto>> = withContext(Dispatchers.IO) {
        val invId = getActiveInvIdOrNull() ?: return@withContext Result.Error(Exception("Identificador de inventário não encontrado."))
        try {
            val response = apiService.getProducts(invId)
            if (response.isSuccessful) {
                val filtered = response.body()?.map { it.toDomain() }
                    ?.filter { it.tipo.id == tipoId } ?: emptyList()
                Result.Success(filtered)
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Erro ao carregar produtos por categoria. Verifique sua conexão."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar produtos por tipo")
            Result.Error(Exception("Erro ao carregar produtos da categoria."))
        }
    }

    override suspend fun editProduto(id: Long, produto: Produto): Result<Produto> = withContext(Dispatchers.IO) {
        val invId = getActiveInvIdOrNull() ?: return@withContext Result.Error(Exception("Identificador de inventário não encontrado."))
        try {
            val response = apiService.updateProduct(invId, id, produto.toUpdateDto())
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it.toDomain())
                } ?: Result.Error(Exception("Erro ao processar a atualização do produto."))
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Erro ao atualizar produto. Verifique sua conexão."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao editar produto")
            Result.Error(Exception("Não foi possível atualizar o produto."))
        }
    }

    override suspend fun getProdutoById(id: Long): Result<Produto> = withContext(Dispatchers.IO) {
        val invId = getActiveInvIdOrNull() ?: return@withContext Result.Error(Exception("Identificador de inventário não encontrado."))
        try {
            val response = apiService.getProducts(invId)
            if (response.isSuccessful) {
                val produto = response.body()?.find { it.id == id }?.toDomain()
                if (produto != null) {
                    Result.Success(produto)
                } else {
                    Result.Error(Exception("Produto não encontrado."))
                }
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Erro ao buscar detalhes do produto. Verifique sua conexão."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar produto")
            Result.Error(Exception("Erro ao carregar detalhes do produto."))
        }
    }

    private fun handleError(response: Response<*>): Result.Error {
        val errorBody = response.errorBody()?.string()
        return try {
            val errorResponse = json.decodeFromString<ErrorResponse>(errorBody ?: "")
            Result.Error(Exception(errorResponse.error.message))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao processar corpo de erro: ${'$'}errorBody")
            val friendlyMessage = when (response.code()) {
                400 -> "Dados inválidos. Verifique as informações preenchidas."
                401 -> "Sessão expirada. Por favor, faça login novamente."
                403 -> "Você não tem permissão para esta ação."
                404 -> "O recurso solicitado não foi encontrado."
                409 -> "Conflito de dados. O item pode já existir."
                500 -> "Erro interno no servidor. Tente novamente em instantes."
                else -> "Ocorreu um erro inesperado no servidor (${'$'}{response.code()})."
            }
            Result.Error(Exception(friendlyMessage))
        }
    }
}
