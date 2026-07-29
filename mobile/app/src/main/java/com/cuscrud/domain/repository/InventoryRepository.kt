package com.cuscrud.domain.repository

import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface de repositório para operações de Inventário.
 */
interface InventoryRepository {

    /**
     * StateFlow que emite o ID do inventário ativo no momento.
     */
    val activeInventoryId: StateFlow<String?>

    /**
     * StateFlow que emite o papel (Role) do usuário no inventário ativo.
     */
    val activeInventoryRole: StateFlow<Role?>

    /**
     * Busca a lista de inventários do usuário.
     */
    suspend fun getInventories(limit: Int = 20, offset: Int = 0): Result<List<InventoryDto>>

    /**
     * Cria um novo inventário.
     */
    suspend fun createInventory(name: String): Result<InventoryDto>

    /**
     * Atualiza o nome de um inventário existente.
     */
    suspend fun updateInventory(invId: String, name: String): Result<InventoryDto>

    /**
     * Deleta um inventário. Caso seja o ativo, ele será limpo.
     */
    suspend fun deleteInventory(invId: String): Result<Unit>

    /**
     * Define o inventário ativo e sua role para o contexto global da aplicação.
     */
    suspend fun setActiveInventory(invId: String, role: Role)

    /**
     * Limpa o inventário ativo selecionado.
     */
    suspend fun clearActiveInventory()
}

/**
 * Retorna true apenas se o papel for OWNER (0).
 */
fun Role?.canManageInventory(): Boolean = this == Role.OWNER

/**
 * Retorna true se o papel for OWNER (0) ou EDITOR (1).
 */
fun Role?.canEditProducts(): Boolean = this == Role.OWNER || this == Role.EDITOR

/**
 * Retorna true para qualquer papel válido (OWNER, EDITOR ou READER).
 */
fun Role?.canViewProducts(): Boolean = this != null
