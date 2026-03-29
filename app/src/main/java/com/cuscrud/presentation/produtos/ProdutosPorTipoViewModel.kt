package com.cuscrud.presentation.produtos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.GetProdutosPorTipoInteractor
import com.cuscrud.domain.interactor.RemoveProdutoInteractor
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProdutosPorTipoViewModel @Inject constructor(
    private val getProdutosPorTipoInteractor: GetProdutosPorTipoInteractor,
    private val removeProdutoInteractor: RemoveProdutoInteractor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tipoId: Long = checkNotNull(savedStateHandle["tipoId"])

    private val _uiState = MutableStateFlow(ProdutosPorTipoUiState())
    val uiState: StateFlow<ProdutosPorTipoUiState> = _uiState.asStateFlow()

    init {
        loadProdutos()
    }

    /**
     * Carrega a lista de produtos do tipo especificado via API.
     */
    fun loadProdutos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getProdutosPorTipoInteractor(tipoId)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(isLoading = false, produtos = result.data, errorMessage = null) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = result.exception.message ?: "Erro ao carregar produtos."
                        ) 
                    }
                }
                Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun solicitarRemocao(produto: Produto) {
        _uiState.update { it.copy(produtoParaRemover = produto) }
    }

    fun cancelarRemocao() {
        _uiState.update { it.copy(produtoParaRemover = null) }
    }

    fun confirmarRemocao() {
        val produtoParaRemover = _uiState.value.produtoParaRemover ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, produtoParaRemover = null) }
            when (val result = removeProdutoInteractor(produtoParaRemover.id)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            mensagemSucesso = "${produtoParaRemover.marca} removido com sucesso"
                        ) 
                    }
                    // Refresh manual da lista após sucesso
                    loadProdutos()
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = result.exception.message ?: "Erro ao excluir produto."
                        ) 
                    }
                }
                Result.Loading -> {}
            }
        }
    }

    fun limparMensagens() {
        _uiState.update { it.copy(errorMessage = null, mensagemSucesso = null) }
    }
}
