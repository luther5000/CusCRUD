package com.cuscrud.data.repository

import com.cuscrud.data.local.SessionManager
import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.CreateInventoryRequest
import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.data.remote.dto.UpdateInventoryRequest
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val apiService: CuscrudApiService,
    private val sessionManager: SessionManager
) : InventoryRepository {

    private val _activeInventoryId = MutableStateFlow<String?>(sessionManager.fetchActiveInventoryId())
    override val activeInventoryId: StateFlow<String?> = _activeInventoryId.asStateFlow()

    private val _activeInventoryRole = MutableStateFlow<Role?>(
        Role.fromInt(sessionManager.fetchActiveInventoryRole())
    )
    override val activeInventoryRole: StateFlow<Role?> = _activeInventoryRole.asStateFlow()

    override suspend fun getInventories(limit: Int, offset: Int): Result<List<InventoryDto>> {
        return try {
            val response = apiService.getInventories(limit, offset)
            if (response.isSuccessful) {
                val inventories = response.body()?.inventories ?: emptyList()
                
                // Se o inventário ativo estiver na lista, atualizamos a role por garantia
                _activeInventoryId.value?.let { activeId ->
                    inventories.find { it.invId == activeId }?.let { activeInv ->
                        val newRole = Role.fromInt(activeInv.role)
                        if (newRole != _activeInventoryRole.value) {
                            _activeInventoryRole.value = newRole
                            newRole?.let { sessionManager.saveActiveInventoryRole(it.value) }
                        }
                    }
                }
                
                Result.Success(inventories)
            } else {
                Result.Error(Exception("Erro ao carregar inventários: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Falha ao buscar inventários")
            Result.Error(e)
        }
    }

    override suspend fun createInventory(name: String): Result<InventoryDto> {
        return try {
            val response = apiService.createInventory(CreateInventoryRequest(name))
            if (response.isSuccessful && response.body() != null) {
                val inventory = response.body()!!
                // Ao criar, o usuário é OWNER (0)
                setActiveInventory(inventory.invId, Role.OWNER)
                Result.Success(inventory)
            } else {
                Result.Error(Exception("Erro ao criar inventário: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Falha ao criar inventário")
            Result.Error(e)
        }
    }

    override suspend fun updateInventory(invId: String, name: String): Result<InventoryDto> {
        return try {
            val response = apiService.updateInventory(invId, UpdateInventoryRequest(name))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(Exception("Erro ao atualizar inventário: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Falha ao atualizar inventário")
            Result.Error(e)
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
                Result.Error(Exception("Erro ao deletar inventário: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Falha ao deletar inventário")
            Result.Error(e)
        }
    }

    override fun setActiveInventory(invId: String, role: Role) {
        sessionManager.saveActiveInventoryId(invId)
        sessionManager.saveActiveInventoryRole(role.value)
        _activeInventoryId.value = invId
        _activeInventoryRole.value = role
    }

    override fun clearActiveInventory() {
        sessionManager.clearActiveInventoryId()
        sessionManager.clearActiveInventoryRole()
        _activeInventoryId.value = null
        _activeInventoryRole.value = null
    }
}
