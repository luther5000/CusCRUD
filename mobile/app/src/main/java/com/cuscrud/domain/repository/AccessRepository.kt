package com.cuscrud.domain.repository

import com.cuscrud.data.remote.dto.UserAccessDto
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.util.Result

/**
 * Interface de repositório para gestão de colaboradores e acessos (RBAC).
 */
interface AccessRepository {

    /**
     * Lista os usuários com acesso ao inventário ativo.
     */
    suspend fun getUsers(limit: Int = 20, offset: Int = 0): Result<List<UserAccessDto>>

    /**
     * Adiciona um novo colaborador ao inventário ativo.
     */
    suspend fun addUser(login: String, role: Role): Result<UserAccessDto>

    /**
     * Atualiza o papel de um colaborador no inventário ativo.
     */
    suspend fun updateUserRole(userId: String, role: Role): Result<UserAccessDto>

    /**
     * Remove o acesso de um colaborador do inventário ativo.
     */
    suspend fun removeUser(userId: String): Result<Unit>
}
