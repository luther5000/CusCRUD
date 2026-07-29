package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.InventoryRepository
import javax.inject.Inject

/**
 * Interactor responsável por definir a ONG (inventário) selecionada como ativa no contexto global do app.
 */
class SetActiveOngInteractor @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(id: String, role: Role) {
        repository.setActiveInventory(id, role)
    }
}
