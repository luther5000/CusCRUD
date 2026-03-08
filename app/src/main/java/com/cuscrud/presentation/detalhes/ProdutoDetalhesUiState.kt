package com.cuscrud.presentation.detalhes

import com.cuscrud.domain.model.Produto

/**
 * Estado da UI para a tela de Detalhes do Produto.
 */
sealed interface ProdutoDetalhesUiState {
    object Loading : ProdutoDetalhesUiState
    data class Success(val produto: Produto?) : ProdutoDetalhesUiState
    data class Error(val message: String) : ProdutoDetalhesUiState
}