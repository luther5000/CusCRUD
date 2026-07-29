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
                handleError(response, "GET_USER")
            }
        } catch (_: IOException) {
            Result.Error(Exception("Não foi possível se conectar ao servidor."))
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
                Result.Success(response.body()!!.user)
            } else {
                handleError(response, "ADD_USER")
            }
        } catch (_: IOException) {
            Result.Error(Exception("Não foi possível se conectar ao servidor."))
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
                Result.Success(response.body()!!.user)
            } else {
                handleError(response, "UPDATE_USER")
            }
        } catch (_: IOException) {
            Result.Error(Exception("Não foi possível se conectar ao servidor."))
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
                handleError(response, "REMOVE_USER")
            }
        } catch (_: IOException) {
            Result.Error(Exception("Não foi possível se conectar ao servidor."))
        } catch (e: Exception) {
            Timber.e(e, "Falha ao remover colaborador")
            Result.Error(e)
        }
    }

    /**
     * Realiza o parse de erros vindos da API priorizando a mensagem enviada pelo servidor.
     */
    private fun handleError(response: Response<*>, context: String): Result.Error {
        val message = when (response.code()) {
            400 -> "Dados inválidos. Verifique as informações preenchidas."
            401 -> "Sessão expirada. Por favor, faça login novamente."
            403 -> "Você não tem permissão para realizar esta ação."
            404 -> when (context) {
                "ADD_USER" -> "Usuário não encontrado no sistema."
                "UPDATE_USER", "REMOVE_USER" -> "Colaborador não encontrado nesta ONG."
                else -> "O recurso solicitado não foi encontrado."
            }
            409 -> when (context) {
                "ADD_USER" -> "Este usuário já faz parte da equipe desta ONG."
                else -> "Conflito na operação. O registro já existe."
            }
            500 -> "Erro interno no servidor. Tente novamente mais tarde."
            else -> "Não foi possível se conectar ao servidor."
        }
        return Result.Error(Exception(message))
    }
}
