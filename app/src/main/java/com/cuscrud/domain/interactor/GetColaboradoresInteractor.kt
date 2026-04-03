package com.cuscrud.domain.interactor

import com.cuscrud.data.remote.dto.UserAccessDto
import com.cuscrud.domain.repository.AccessRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor responsável por buscar a lista de colaboradores de uma ONG.
 */
class GetColaboradoresInteractor @Inject constructor(
    private val repository: AccessRepository
) {
    suspend operator fun invoke(): Result<List<UserAccessDto>> {
        return repository.getUsers()
    }
}
