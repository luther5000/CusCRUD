package com.cuscrud.presentation.ong

import com.cuscrud.data.remote.dto.UserAccessDto
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
    val showDeleteConfirmation: Boolean = false,
    
    // Gestão de Colaboradores
    val colaboradores: List<UserAccessDto> = emptyList(),
    val isLoadingColaboradores: Boolean = false,
    val showAddColaboradorDialog: Boolean = false,
    val addColaboradorEmail: String = "",
    val addColaboradorRole: Role = Role.EDITOR,
    val isAddingColaborador: Boolean = false
)
