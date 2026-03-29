package com.cuscrud.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.GetInventarioAgrupadoInteractor
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventarioViewModel @Inject constructor(
    private val getInventarioAgrupadoInteractor: GetInventarioAgrupadoInteractor
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventarioUiState>(InventarioUiState.Loading)
    val uiState: StateFlow<InventarioUiState> = _uiState.asStateFlow()

    init {
        fetchInventario()
    }

    /**
     * Busca o inventário agrupado da API remota.
     * Como não há mais SSOT local, esta função deve ser chamada manualmente para refresh.
     */
    fun fetchInventario() {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            when (val result = getInventarioAgrupadoInteractor()) {
                is Result.Success -> {
                    _uiState.value = InventarioUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = InventarioUiState.Error(
                        result.exception.message ?: "Erro: Não foi possível carregar os dados do inventário."
                    )
                }
                Result.Loading -> {
                    _uiState.value = InventarioUiState.Loading
                }
            }
        }
    }
}
