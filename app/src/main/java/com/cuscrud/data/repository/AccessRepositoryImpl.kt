package com.cuscrud.data.repository

import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.*
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.AccessRepository
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import kotlinx.serialization.json.Json
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Implementação do repositório [AccessRepository] responsável por gerenciar as permissões e colaboradores
 * de um inventário no sistema CusCRUD.
 *
 * Esta classe atua como uma ponte entre a camada de dados (API remota) e a camada de domínio,
 * realizando as seguintes operações:
 * - **Listagem**: Busca a lista de usuários com acesso ao inventário ativo.
 * - **Adição**: Convida ou adiciona novos colaboradores via login.
 * - **Atualização**: Modifica o nível de acesso (Role) de um colaborador existente.
 * - **Remoção**: Revoga o acesso de um usuário ao inventário.
 *
 * Depende de [InventoryRepository] para identificar qual o inventário está atualmente ativo
 * para as operações de contexto.
 */

class AccessRepositoryImpl @Inject constructor(
    private val apiService: CuscrudApiService,
    private val inventoryRepository: InventoryRepository,
    private val json: Json
) : AccessRepository {

    override suspend fun getUsers(limit: Int, offset: Int): Result<List<UserAccessDto>> {
        val invId = inventoryRepository.activeInventoryId.value
            ?: return Result.Error(Exception("Nenhum inventário ativo selecionado"))

        return try {
            val response = apiService.getInventoryUsers(invId, limit, offset)
            if (response.isSuccessful) {
                Result.Success(response.body()?.users ?: emptyList())
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Falha de conexão. Verifique sua internet."))
        } catch (e: Exception) {
            Timber.e(e, "Falha ao buscar colaboradores")
            Result.Error(e)
        }
    }

    override suspend fun addUser(login: String, role: Role): Result<UserAccessDto> {
        val invId = inventoryRepository.activeInventoryId.value
            ?: return Result.Error(Exception("Nenhum inventário ativo selecionado"))

        return try {
            val response = apiService.addInventoryUser(invId, AddUserAccessRequest(login, role.value))
            if (response.isSuccessful && response.body() != null) {
                // Desembrulha o UserAccessDto de dentro do UserAccessResponse
                Result.Success(response.body()!!.user)
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Falha de conexão ao adicionar colaborador."))
        } catch (e: Exception) {
            Timber.e(e, "Falha ao adicionar colaborador")
            Result.Error(e)
        }
    }

    override suspend fun updateUserRole(userId: String, role: Role): Result<UserAccessDto> {
        val invId = inventoryRepository.activeInventoryId.value
            ?: return Result.Error(Exception("Nenhum inventário ativo selecionado"))

        return try {
            val response = apiService.updateInventoryUserRole(invId, userId, UpdateUserAccessRequest(role.value))
            if (response.isSuccessful && response.body() != null) {
                // Desembrulha o UserAccessDto de dentro do UserAccessResponse
                Result.Success(response.body()!!.user)
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Falha de conexão ao atualizar papel."))
        } catch (e: Exception) {
            Timber.e(e, "Falha ao atualizar papel")
            Result.Error(e)
        }
    }

    override suspend fun removeUser(userId: String): Result<Unit> {
        val invId = inventoryRepository.activeInventoryId.value
            ?: return Result.Error(Exception("Nenhum inventário ativo selecionado"))

        return try {
            val response = apiService.removeInventoryUser(invId, userId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Falha de conexão ao remover colaborador."))
        } catch (e: Exception) {
            Timber.e(e, "Falha ao remover colaborador")
            Result.Error(e)
        }
    }

    /**
     * Realiza o parse de erros vindos da API seguindo o padrão { "error": { "code": "...", "message": "..." } }
     * definido no architecture.md.
     */
    private fun handleError(response: Response<*>): Result.Error {
        // Prioridade 1: Mapeamento Estilizado por Contexto (Repositório de Acessos)
        val friendlyMessage = when (response.code()) {
            400 -> "Dados inválidos. Verifique o e-mail informado."
            401 -> "Sessão expirada. Por favor, faça login novamente."
            403 -> "Você não tem permissão para gerenciar usuários neste inventário."
            404 -> "O usuário ou inventário não foi encontrado."
            409 -> "Este usuário já possui acesso ou a operação não é permitida."
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
