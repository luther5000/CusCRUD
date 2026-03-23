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
import kotlinx.coroutines.flow.collectLatest
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

    private fun loadProdutos() {
        viewModelScope.launch {
            getProdutosPorTipoInteractor(tipoId).collectLatest { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update { 
                        it.copy(isLoading = false, produtos = result.data, errorMessage = null) 
                    }
                    is Result.Error -> _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = result.exception.message ?: "Erro: Não foi possivel carregar os dados dos produtos desse tipo, tente novamente mais tarde."
                        ) 
                    }
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
            val produtoDeletado = removeProdutoInteractor(produtoParaRemover)
            
            if (produtoDeletado != null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        mensagemSucesso = "${produtoDeletado.marca} removido com sucesso"
                    ) 
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Não foi possível realizar a remoção: Produto não encontrado ou erro no banco." 
                    )
                }
            }
        }
    }

    fun limparMensagens() {
        _uiState.update { it.copy(errorMessage = null, mensagemSucesso = null) }
    }
}