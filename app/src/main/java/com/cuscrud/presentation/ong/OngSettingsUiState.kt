package com.cuscrud.presentation.ong

import com.cuscrud.domain.model.Role

/**
 * Representa o estado da UI para a tela de Definições da ONG.
 */
data class OngSettingsUiState(
    val ongId: String = "",
    val ongName: String = "",
    val userRole: Role? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val editName: String = "",
    val userMessage: String? = null,
    val isSuccess: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)
