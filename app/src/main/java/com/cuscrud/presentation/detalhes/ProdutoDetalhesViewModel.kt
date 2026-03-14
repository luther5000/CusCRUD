package com.cuscrud.presentation.detalhes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.GetProdutoDetalhesInteractor
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProdutoDetalhesViewModel @Inject constructor(
    getProdutoDetalhesInteractor: GetProdutoDetalhesInteractor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val produtoId: Int = checkNotNull(savedStateHandle["produtoId"])

    val uiState: StateFlow<ProdutoDetalhesUiState> = getProdutoDetalhesInteractor(produtoId)
        .map { result ->
            when (result) {
                is Result.Success -> ProdutoDetalhesUiState.Success(result.data)
                is Result.Error -> ProdutoDetalhesUiState.Error(result.exception.message ?: "Erro: Não foi possivel recuperar os dados desse produto, tente novamente mais tarde.")
                is Result.Loading -> ProdutoDetalhesUiState.Loading
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProdutoDetalhesUiState.Loading
        )
}