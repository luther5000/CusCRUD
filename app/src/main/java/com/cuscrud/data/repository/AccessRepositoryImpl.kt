package com.cuscrud.data.repository

import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.AddUserAccessRequest
import com.cuscrud.data.remote.dto.UpdateUserAccessRequest
import com.cuscrud.data.remote.dto.UserAccessDto
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.AccessRepository
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import timber.log.Timber
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
    private val inventoryRepository: InventoryRepository
) : AccessRepository {

    override suspend fun getUsers(limit: Int, offset: Int): Result<List<UserAccessDto>> {
        val invId = inventoryRepository.activeInventoryId.value
            ?: return Result.Error(Exception("Nenhum inventário ativo selecionado"))

        return try {
            val response = apiService.getInventoryUsers(invId, limit, offset)
            if (response.isSuccessful) {
                Result.Success(response.body()?.users ?: emptyList())
            } else {
                Result.Error(Exception("Erro ao buscar colaboradores: ${response.code()}"))
            }
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
                Result.Success(response.body()!!)
            } else {
                Result.Error(Exception("Erro ao adicionar colaborador: ${response.code()}"))
            }
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
                Result.Success(response.body()!!)
            } else {
                Result.Error(Exception("Erro ao atualizar papel: ${response.code()}"))
            }
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
                Result.Error(Exception("Erro ao remover colaborador: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Falha ao remover colaborador")
            Result.Error(e)
        }
    }
}
