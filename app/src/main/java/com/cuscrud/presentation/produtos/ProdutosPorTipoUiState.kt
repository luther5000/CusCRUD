package com.cuscrud.presentation.produtos

import com.cuscrud.domain.model.Produto

/**
 * Estado da UI para a tela de Produtos por Tipo.
 */
data class ProdutosPorTipoUiState(
    val isLoading: Boolean = false,
    val produtos: List<Produto> = emptyList(),
    val errorMessage: String? = null,
    val produtoParaRemover: Produto? = null,
    val mensagemSucesso: String? = null
)