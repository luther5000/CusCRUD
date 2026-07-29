package com.cuscrud.presentation.ong

/**
 * Estado da UI para a tela de Criação de ONG.
 */
data class CreateOngUiState(
    val name: String = "",
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val isOngCreated: Boolean = false
)
