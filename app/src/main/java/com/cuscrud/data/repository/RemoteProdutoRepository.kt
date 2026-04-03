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
 * 
 * Segue as especificações da Seção 5.5 do documento de arquitetura.
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
                // Desembrulha a lista de produtos do wrapper ProdutoListResponse
                val products = response.body()?.products?.map { it.toDomain() } ?: emptyList()
                Result.Success(products)
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
            // Utiliza o endpoint especializado da API (Seção 5.5.3) em vez de filtrar localmente
            val response = apiService.getProductsByType(invId, tipoId)
            if (response.isSuccessful) {
                val products = response.body()?.products?.map { it.toDomain() } ?: emptyList()
                Result.Success(products)
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
            // Utiliza o endpoint especializado da API (Seção 5.5.2)
            val response = apiService.getProductById(invId, id)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it.toDomain())
                } ?: Result.Error(Exception("Produto não encontrado."))
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

    /**
     * Realiza o parse de erros vindos da API seguindo o padrão { "error": { "code": "...", "message": "..." } }
     * definido no architecture.md.
     */
    private fun handleError(response: Response<*>): Result.Error {
        // Prioridade 1: Mapeamento Estilizado por Contexto (Repositório de Produtos)
        val friendlyMessage = when (response.code()) {
            400 -> "Dados inválidos. Verifique as informações do produto."
            401 -> "Sessão expirada. Por favor, faça login novamente."
            403 -> "Você não tem permissão de escrita para este inventário."
            404 -> "O produto ou inventário não foi encontrado."
            409 -> "Conflito de dados. Verifique se o item já existe."
            500 -> "Erro interno no servidor. Tente novamente em instantes."
            else -> null
        }

        if (friendlyMessage != null) {
            return Result.Error(Exception(friendlyMessage))
        }

        // Prioridade 2: Fallback para a mensagem do servidor
        val errorBody = response.errorBody()?.string()
        return try {
            val errorResponse = json.decodeFromString<ErrorResponse>(errorBody ?: "")
            Result.Error(Exception(errorResponse.error.message))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao processar corpo de erro: $errorBody")
            Result.Error(Exception("Ocorreu um erro inesperado no servidor (${response.code()})."))
        }
    }
}
