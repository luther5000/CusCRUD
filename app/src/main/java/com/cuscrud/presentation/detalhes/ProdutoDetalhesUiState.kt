package com.cuscrud.presentation.detalhes

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Role

/**
 * Estado da UI para a tela de Detalhes do Produto.
 */
data class ProdutoDetalhesUiState(
    val isLoading: Boolean = false,
    val produto: Produto? = null,
    val userMessage: String? = null,
    val userRole: Role? = null,
    val isUpdatingQuantity: Boolean = false
)
