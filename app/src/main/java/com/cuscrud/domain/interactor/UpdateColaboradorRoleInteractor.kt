package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.AccessRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor responsável por atualizar o papel (permissão) de um colaborador em um inventário.
 * Encapsula a lógica de negócio de mudança de nível de acesso (RBAC).
 */
class UpdateColaboradorRoleInteractor @Inject constructor(
    private val repository: AccessRepository
) {
    /**
     * Atualiza o papel do colaborador.
     * @param userId Identificador único do usuário.
     * @param newRole Novo papel a ser atribuído (Ex: EDITOR, READER).
     */
    suspend operator fun invoke(userId: String, newRole: Role): Result<Unit> {
        return when (val result = repository.updateUserRole(userId, newRole)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.exception)
            is Result.Loading -> Result.Loading
        }
    }
}
