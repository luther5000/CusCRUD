package com.cuscrud.presentation.produtos

import com.cuscrud.domain.model.Produto

/**
 * Estado da UI para a tela de Produtos por Tipo.
 */
sealed interface ProdutosPorTipoUiState {
    object Loading : ProdutosPorTipoUiState
    data class Success(val produtos: List<Produto>) : ProdutosPorTipoUiState
    data class Error(val message: String) : ProdutosPorTipoUiState
}