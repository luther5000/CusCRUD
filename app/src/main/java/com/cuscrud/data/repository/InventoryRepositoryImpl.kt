
package com.cuscrud.data.repository

import com.cuscrud.data.local.SessionManager
import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.*
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do repositório [InventoryRepository] responsável por gerenciar os inventários e o estado
 * do inventário ativo no sistema CusCRUD.
 *
 * Esta classe centraliza a lógica de:
 * - **Listagem**: Busca todos os inventários aos quais o usuário tem acesso.
 * - **Criação e Gestão**: Permite criar, atualizar e excluir inventários, tratando erros de rede e persistência.
 * - **Estado Ativo**: Mantém o ID e o papel (Role) do inventário selecionado atualmente, persistindo essas
 *   informações via [SessionManager] e expondo-as através de [StateFlow].
 * - **Sincronização**: Garante que mudanças no armazenamento local (DataStore) sejam refletidas em tempo real na UI.
 * - **Tratamento de Erros**: Converte respostas HTTP em mensagens de erro amigáveis para o usuário.
 *
 * Sendo um `@Singleton`, garante uma fonte única de verdade para o inventário selecionado em toda a aplicação.
 */

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val apiService: CuscrudApiService,
    private val sessionManager: SessionManager,
    private val json: Json
) : InventoryRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _activeInventoryId = MutableStateFlow<String?>(null)
    override val activeInventoryId: StateFlow<String?> = _activeInventoryId.asStateFlow()

    private val _activeInventoryRole = MutableStateFlow<Role?>(null)
    override val activeInventoryRole: StateFlow<Role?> = _activeInventoryRole.asStateFlow()

    init {
        // Observa mudanças no DataStore e reflete no StateFlow do repositório
        repositoryScope.launch {
            sessionManager.activeInventoryIdFlow.collect { id ->
                _activeInventoryId.value = id
            }
        }
        repositoryScope.launch {
            sessionManager.activeInventoryRoleFlow.collect { roleInt ->
                _activeInventoryRole.value = Role.fromInt(roleInt)
            }
        }
    }

    override suspend fun getInventories(limit: Int, offset: Int): Result<List<InventoryDto>> {
        return try {
            val response = apiService.getInventories(limit, offset)
            if (response.isSuccessful) {
                val inventories = response.body()?.inventories ?: emptyList()
                
                _activeInventoryId.value?.let { activeId ->
                    inventories.find { it.invId == activeId }?.let { activeInv ->
                        // Correção do erro de Argument type mismatch:
                        // activeInv.role pode ser nulo no DTO, mas o enum Role.fromInt espera Int.
                        // Usamos o operador elvis para prover um valor padrão ou tratamos a nulidade.
                        activeInv.role?.let { roleInt ->
                            val newRole = Role.fromInt(roleInt)
                            if (newRole != _activeInventoryRole.value) {
                                newRole?.let { sessionManager.saveActiveInventoryRole(it.value) }
                            }
                        }
                    }
                }
                
                Result.Success(inventories)
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Falha de conexão ao buscar inventários."))
        } catch (e: Exception) {
            Timber.e(e, "Falha ao buscar inventários")
            Result.Error(Exception("Não foi possível carregar a lista de inventários."))
        }
    }

    override suspend fun createInventory(name: String): Result<InventoryDto> {
        return try {
            val response = apiService.createInventory(CreateInventoryRequest(name))
            if (response.isSuccessful && response.body() != null) {
                val inventory = response.body()!!.inventory
                setActiveInventory(inventory.invId, Role.OWNER)
                Result.Success(inventory)
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Falha de conexão ao criar inventário."))
        } catch (_: Exception) {
            Result.Error(Exception("Erro ao criar inventário."))
        }
    }

    override suspend fun updateInventory(invId: String, name: String): Result<InventoryDto> {
        return try {
            val response = apiService.updateInventory(invId, UpdateInventoryRequest(name))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.inventory)
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Falha de conexão ao atualizar."))
        } catch (_: Exception) {
            Result.Error(Exception("Erro ao atualizar inventário."))
        }
    }

    override suspend fun deleteInventory(invId: String): Result<Unit> {
        return try {
            val response = apiService.deleteInventory(invId)
            if (response.isSuccessful) {
                if (_activeInventoryId.value == invId) {
                    clearActiveInventory()
                }
                Result.Success(Unit)
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Falha de conexão ao excluir."))
        } catch (_: Exception) {
            Result.Error(Exception("Erro ao excluir inventário."))
        }
    }

    override suspend fun setActiveInventory(invId: String, role: Role) {
        sessionManager.saveActiveInventoryId(invId)
        sessionManager.saveActiveInventoryRole(role.value)
    }

    override suspend fun clearActiveInventory() {
        sessionManager.clearActiveInventoryId()
        sessionManager.clearActiveInventoryRole()
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
                404 -> "O inventário ou recurso não foi encontrado."
                409 -> "Conflito de dados. Talvez este nome já esteja em uso."
                500 -> "Erro interno no servidor. Tente novamente em instantes."
                else -> "Ocorreu um erro inesperado (Código: ${response.code()})"
            }
            Result.Error(Exception(friendlyMessage))
        }
    }
}
