package com.cuscrud.domain.interactor

import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor responsável por criar uma nova ONG (inventário).
 */
class CreateOngInteractor @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(name: String): Result<InventoryDto> {
        if (name.isBlank()) {
            return Result.Error(IllegalArgumentException("O nome da ONG não pode estar em branco."))
        }
        return repository.createInventory(name)
    }
}
