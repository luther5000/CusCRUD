package com.cuscrud.domain.interactor

import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor responsável por remover uma ONG (inventário) do sistema.
 */
class DeleteOngInteractor @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(ongId: String): Result<Unit> {
        if (ongId.isBlank()) {
            return Result.Error(IllegalArgumentException("ID da ONG inválido."))
        }
        
        val result = repository.deleteInventory(ongId)
        
        if (result is Result.Success) {
            // Se a remoção for bem-sucedida, limpamos o contexto ativo localmente
            repository.clearActiveInventory()
        }
        
        return result
    }
}
