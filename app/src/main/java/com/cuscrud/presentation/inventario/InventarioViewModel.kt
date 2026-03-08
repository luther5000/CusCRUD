package com.cuscrud.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.GetInventarioAgrupadoInteractor
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InventarioViewModel @Inject constructor(
    getInventarioAgrupadoInteractor: GetInventarioAgrupadoInteractor
) : ViewModel() {

    /**
     * Estado da UI derivado diretamente do Interactor.
     * O uso de stateIn garante que o fluxo seja mantido durante mudanças de configuração.
     */
    val uiState: StateFlow<InventarioUiState> = getInventarioAgrupadoInteractor()
        .map { result ->
            when (result) {
                is Result.Success -> InventarioUiState.Success(result.data)
                is Result.Error -> InventarioUiState.Error(result.exception.message ?: "Erro desconhecido")
                is Result.Loading -> InventarioUiState.Loading
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InventarioUiState.Loading
        )
}