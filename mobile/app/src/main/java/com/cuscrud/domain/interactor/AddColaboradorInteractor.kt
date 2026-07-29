package com.cuscrud.domain.interactor

import com.cuscrud.data.remote.dto.UserAccessDto
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.AccessRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor responsável por adicionar um colaborador a uma ONG.
 */
class AddColaboradorInteractor @Inject constructor(
    private val repository: AccessRepository
) {
    suspend operator fun invoke(email: String, role: Role): Result<UserAccessDto> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.Error(IllegalArgumentException("E-mail inválido."))
        }
        
        if (role == Role.OWNER) {
            return Result.Error(IllegalArgumentException("Não é possível adicionar outro Dono."))
        }

        return repository.addUser(email, role)
    }
}
