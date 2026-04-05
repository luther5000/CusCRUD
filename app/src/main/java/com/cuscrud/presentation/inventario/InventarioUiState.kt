package com.cuscrud.presentation.inventario

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.model.Tipo

/**
 * Estado da UI para a tela de Inventário Geral.
 */
sealed interface InventarioUiState {
    object Loading : InventarioUiState
    data class Success(
        val inventario: Map<Tipo, List<Produto>> = emptyMap(),
        val userRole: Role? = null
    ) : InventarioUiState
    data class Error(val message: String) : InventarioUiState
}
