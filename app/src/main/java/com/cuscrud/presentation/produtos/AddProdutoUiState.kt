package com.cuscrud.presentation.produtos

import com.cuscrud.domain.model.Role
import com.cuscrud.domain.model.Tipo
import java.util.Date

/**
 * Estado da UI para a tela de adicionar/editar produto.
 */
data class AddProdutoUiState(
    val marca: String = "",
    val unidade: String = "",
    val unidadeMedida: String = "",
    val quantidade: String = "",
    val dataValidade: Date = Date(),
    val tipoSelecionado: Tipo? = null,
    val tipos: List<Tipo> = emptyList(),
    val unidadesMedida: List<String> = listOf("kg", "g", "l", "ml", "un", "pacote", "lata", "caixa"),
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val isProductSaved: Boolean = false,
    val isEditMode: Boolean = false,
    val userRole: Role? = null
)
