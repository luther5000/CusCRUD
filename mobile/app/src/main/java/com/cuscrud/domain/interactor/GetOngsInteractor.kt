package com.cuscrud.domain.interactor

import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor responsável por buscar a lista de ONGs (inventários) que o usuário possui acesso.
 */
class GetOngsInteractor @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(): Result<List<InventoryDto>> {
        return repository.getInventories()
    }
}
