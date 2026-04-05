package com.cuscrud.presentation.ong

import com.cuscrud.data.remote.dto.InventoryDto

/**
 * Representa o estado da UI para a tela de Seleção de ONG.
 */
data class SelectOngUiState(
    val ongs: List<InventoryDto> = emptyList(),
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val isOngSelected: Boolean = false
)
