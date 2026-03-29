
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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do repositório [ProdutoRepository] que gerencia as operações de produtos via API remota.
 *
 * Esta classe é responsável por:
 * - **Listagem e Filtro**: Recupera todos os produtos ou filtra por tipo, sempre vinculados ao inventário ativo.
 * - **Operações de Escrita**: Insere, edita e remove produtos através do [CuscrudApiService].
 * - **Gerenciamento de Contexto**: Obtém dinamicamente o ID do inventário ativo a partir do [InventoryRepository].
 * - **Tratamento de Erros**: Centraliza a lógica de tratamento de respostas HTTP, convertendo-as em
 *   mensagens legíveis ou exceções de negócio conforme definido no architecture.md.
 * - **Reatividade**: Expõe dados através de [Flow], garantindo que as chamadas sejam executadas no [Dispatchers.IO].
 *
 * Sendo um `@Singleton`, garante a consistência das operações de produtos em toda a aplicação.
 */

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
            val invId = getActiveInvIdOrNull() ?: throw Exception("Nenhum inventário ativo selecionado.")
            val response = apiService.getProducts(invId)
            if (response.isSuccessful) {
                emit(response.body()?.map { it.toDomain() } ?: emptyList())
            } else {
                val errorMsg = parseErrorMessage(response)
                throw Exception(errorMsg)
            }
        } catch (_: IOException) {
            throw Exception("Falha de conexão. Verifique sua internet.")
        } catch (e: Exception) {
            throw e
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun insertProduto(produto: Produto) {
        try {
            val invId = getActiveInvIdOrNull() ?: throw Exception("Identificador de inventário não encontrado.")
            val response = apiService.addProduct(invId, produto.toRequestDto())
            if (!response.isSuccessful) {
                throw Exception(parseErrorMessage(response))
            }
        } catch (_: IOException) {
            throw Exception("Não foi possível salvar o produto. Verifique sua conexão.")
        }
    }

    override suspend fun removeProduto(id: Int): Produto? {
        try {
            val invId = getActiveInvIdOrNull() ?: throw Exception("Identificador de inventário não encontrado.")
            val response = apiService.deleteProduct(invId, id)
            if (response.isSuccessful) {
                return null // Ou retornar o produto se a API suportar
            } else {
                throw Exception(parseErrorMessage(response))
            }
        } catch (_: IOException) {
            throw Exception("Erro ao excluir produto. Verifique sua conexão.")
        }
    }

    override fun getProdutosByTipo(tipoId: Long): Flow<List<Produto>> = flow {
        try {
            val invId = getActiveInvIdOrNull() ?: throw Exception("Identificador de inventário não encontrado.")
            val response = apiService.getProducts(invId)
            if (response.isSuccessful) {
                val filtered = response.body()?.map { it.toDomain() }
                    ?.filter { it.tipo.id == tipoId } ?: emptyList()
                emit(filtered)
            } else {
                throw Exception(parseErrorMessage(response))
            }
        } catch (_: IOException) {
            throw Exception("Erro ao carregar produtos por categoria.")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun editProduto(id: Int, produto: Produto): Produto? {
        try {
            val invId = getActiveInvIdOrNull() ?: throw Exception("Identificador de inventário não encontrado.")
            val response = apiService.updateProduct(invId, id, produto.toUpdateDto())
            if (response.isSuccessful) {
                return response.body()?.toDomain()
            } else {
                throw Exception(parseErrorMessage(response))
            }
        } catch (_: IOException) {
            throw Exception("Erro ao atualizar produto. Verifique sua conexão.")
        }
    }

    override suspend fun getProdutoById(id: Int): Produto? {
        try {
            val invId = getActiveInvIdOrNull() ?: throw Exception("Identificador de inventário não encontrado.")
            val response = apiService.getProducts(invId)
            if (response.isSuccessful) {
                return response.body()?.find { it.id == id }?.toDomain()
            } else {
                throw Exception(parseErrorMessage(response))
            }
        } catch (_: IOException) {
            throw Exception("Erro ao buscar detalhes do produto.")
        }
    }

    private fun parseErrorMessage(response: retrofit2.Response<*>): String {
        val errorBody = response.errorBody()?.string()
        return try {
            // Tenta decodificar o erro conforme o padrão da arquitetura { "error": { "code": "...", "message": "..." } }
            val errorResponse =
                Json.decodeFromString<com.cuscrud.data.remote.dto.ErrorResponse>(errorBody ?: "")
            errorResponse.error.message
        } catch (_: Exception) {
            // Fallback baseado nos códigos de erro da arquitetura (Seção 5)
            when (response.code()) {
                400 -> "Dados inválidos. Verifique as informações preenchidas." // VALIDATION_ERROR
                401 -> "Sessão expirada. Por favor, faça login novamente."      // UNAUTHENTICATED
                403 -> "Você não tem permissão para esta ação."                // FORBIDDEN
                404 -> "O recurso solicitado não foi encontrado."               // NOT_FOUND
                409 -> "Conflito de dados. O item pode já existir."              // CONFLICT
                else -> "Ocorreu um erro inesperado no servidor (${response.code()})."
            }
        }
    }
}
