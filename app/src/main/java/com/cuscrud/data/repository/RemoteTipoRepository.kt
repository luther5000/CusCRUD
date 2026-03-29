package com.cuscrud.data.repository

import com.cuscrud.data.mapper.toDomain
import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.CreateTipoRequest
import com.cuscrud.data.remote.dto.ErrorResponse
import com.cuscrud.data.remote.dto.UpdateTipoRequest
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.repository.TipoRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Implementação do repositório [TipoRepository] responsável pela gestão remota das categorias (tipos) de produtos.
 *
 * Esta classe centraliza a lógica de:
 * - **Listagem Paginada**: Busca as categorias vinculadas ao inventário ativo no backend.
 * - **Operações de CRUD**: Gerencia a criação, edição e exclusão de tipos de produtos, incluindo o suporte a imagens em Base64.
 * - **Gestão de Contexto**: Integra-se com o [InventoryRepository] para recuperar dinamicamente o ID do inventário ativo.
 * - **Tratamento de Erros**: Segue o padrão da arquitetura para converter respostas HTTP (como o erro 409 ao excluir tipos com produtos vinculados)
 *   em resultados amigáveis ([Result]) para a UI.
 * - **Threading**: Executa todas as chamadas de rede de forma assíncrona utilizando [Dispatchers.IO].
 *
 * Segue as especificações da Seção 5.4 do documento de arquitetura.
 */

class RemoteTipoRepository @Inject constructor(
    private val apiService: CuscrudApiService,
    private val inventoryRepository: InventoryRepository,
    private val json: Json
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
                handleError(response)
            }
        } catch (e: IOException) {
            Result.Error(Exception("Falha de conexão ao buscar categorias. Verifique sua internet."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar tipos")
            Result.Error(Exception("Não foi possível carregar as categorias."))
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
                } ?: Result.Error(Exception("Categoria não encontrada."))
            } else {
                handleError(response)
            }
        } catch (e: IOException) {
            Result.Error(Exception("Falha de conexão. Verifique sua internet."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar tipo por id: $id")
            Result.Error(Exception("Não foi possível carregar os detalhes da categoria."))
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
                } ?: Result.Error(Exception("Erro ao processar a criação da categoria."))
            } else {
                handleError(response)
            }
        } catch (e: IOException) {
            Result.Error(Exception("Falha de conexão ao criar categoria."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao criar tipo")
            Result.Error(Exception("Ocorreu um erro ao tentar salvar a categoria."))
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
                } ?: Result.Error(Exception("Erro ao processar a atualização da categoria."))
            } else {
                handleError(response)
            }
        } catch (e: IOException) {
            Result.Error(Exception("Falha de conexão ao atualizar categoria."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao editar tipo id: $id")
            Result.Error(Exception("Não foi possível atualizar a categoria."))
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
            } else {
                handleError(response)
            }
        } catch (e: IOException) {
            Result.Error(Exception("Falha de conexão ao excluir categoria."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao remover tipo id: $id")
            Result.Error(Exception("Ocorreu um erro ao tentar excluir a categoria."))
        }
    }

    /**
     * Realiza o parse de erros vindos da API seguindo o padrão { "error": { "code": "...", "message": "..." } }
     * definido no architecture.md.
     */
    private fun handleError(response: Response<*>): Result.Error {
        val errorBody = response.errorBody()?.string()
        return try {
            val errorResponse = json.decodeFromString<ErrorResponse>(errorBody ?: "")
            Result.Error(Exception(errorResponse.error.message))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao processar corpo de erro: $errorBody")
            val friendlyMessage = when (response.code()) {
                400 -> "Dados inválidos. Verifique as informações preenchidas."
                401 -> "Sessão expirada. Por favor, faça login novamente."
                403 -> "Você não tem permissão para esta ação."
                404 -> "A categoria não foi encontrada."
                409 -> "Não é possível excluir este tipo pois existem produtos vinculados a ele."
                500 -> "Erro interno no servidor. Tente novamente em instantes."
                else -> "Ocorreu um erro inesperado (Código: ${response.code()})"
            }
            Result.Error(Exception(friendlyMessage))
        }
    }
}
