package com.cuscrud.presentation.detalhes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.GetProdutoDetalhesInteractor
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProdutoDetalhesViewModel @Inject constructor(
    private val getProdutoDetalhesInteractor: GetProdutoDetalhesInteractor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val produtoId: Int = checkNotNull(savedStateHandle["produtoId"])

    private val _uiState = MutableStateFlow<ProdutoDetalhesUiState>(ProdutoDetalhesUiState.Loading)
    val uiState: StateFlow<ProdutoDetalhesUiState> = _uiState.asStateFlow()

    init {
        loadProduto()
    }

    fun loadProduto() {
        viewModelScope.launch {
            _uiState.value = ProdutoDetalhesUiState.Loading
            when (val result = getProdutoDetalhesInteractor(produtoId)) {
                is Result.Success -> {
                    _uiState.value = ProdutoDetalhesUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = ProdutoDetalhesUiState.Error(
                        result.exception.message ?: "Erro ao carregar detalhes do produto."
                    )
                }
                Result.Loading -> {
                    _uiState.value = ProdutoDetalhesUiState.Loading
                }
            }
        }
    }
}
