package com.cuscrud.presentation.produtos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.GetProdutosPorTipoInteractor
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProdutosPorTipoViewModel @Inject constructor(
    private val getProdutosPorTipoInteractor: GetProdutosPorTipoInteractor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Obtém o tipoId da navegação. O nome da chave deve coincidir com a rota definida no NavHost.
    private val tipoId: Long = checkNotNull(savedStateHandle["tipoId"])

    val uiState: StateFlow<ProdutosPorTipoUiState> = getProdutosPorTipoInteractor(tipoId)
        .map { result ->
            when (result) {
                is Result.Success -> ProdutosPorTipoUiState.Success(result.data)
                is Result.Error -> ProdutosPorTipoUiState.Error(result.exception.message ?: "Erro: Não foi possivel carregar os dados dos produtos desse tipo, tente novamente mais tarde.")
                is Result.Loading -> ProdutosPorTipoUiState.Loading
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProdutosPorTipoUiState.Loading
        )
}