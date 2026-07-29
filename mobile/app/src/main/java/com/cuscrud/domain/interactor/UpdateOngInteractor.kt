package com.cuscrud.domain.interactor

import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor responsável por atualizar os dados de uma ONG (nome).
 */
class UpdateOngInteractor @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(invId: String, newName: String): Result<InventoryDto> {
        if (newName.isBlank()) {
            return Result.Error(IllegalArgumentException("O preenchimento do nome é obrigatório."))
        }
        return repository.updateInventory(invId, newName)
    }
}
