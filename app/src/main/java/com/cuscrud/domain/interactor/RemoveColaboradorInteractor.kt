package com.cuscrud.domain.interactor

import com.cuscrud.domain.repository.AccessRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor responsável por remover o acesso de um colaborador a um inventário.
 * Encapsula a lógica de negócio de revogação de acesso (RBAC).
 */
class RemoveColaboradorInteractor @Inject constructor(
    private val repository: AccessRepository
) {
    /**
     * Remove o colaborador do inventário ativo.
     * @param userId Identificador único do usuário a ser removido.
     */
    suspend operator fun invoke(userId: String): Result<Unit> {
        return repository.removeUser(userId)
    }
}
