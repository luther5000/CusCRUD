package com.cuscrud.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.GetInventarioAgrupadoInteractor
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventarioViewModel @Inject constructor(
    private val getInventarioAgrupadoInteractor: GetInventarioAgrupadoInteractor,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventarioUiState>(InventarioUiState.Loading)
    val uiState: StateFlow<InventarioUiState> = _uiState.asStateFlow()

    init {
        fetchInventario()
        observeRole()
    }

    private fun observeRole() {
        viewModelScope.launch {
            inventoryRepository.activeInventoryRole.collect { role ->
                _uiState.update { state ->
                    if (state is InventarioUiState.Success) {
                        state.copy(userRole = role)
                    } else {
                        state
                    }
                }
            }
        }
    }

    /**
     * Busca o inventário agrupado da API remota.
     * Como não há mais SSOT local, esta função deve ser chamada manualmente para refresh.
     */
    fun fetchInventario() {
        viewModelScope.launch {
            val currentRole = (uiState.value as? InventarioUiState.Success)?.userRole
            
            _uiState.value = InventarioUiState.Loading
            when (val result = getInventarioAgrupadoInteractor()) {
                is Result.Success -> {
                    _uiState.value = InventarioUiState.Success(
                        inventario = result.data,
                        userRole = currentRole ?: inventoryRepository.activeInventoryRole.value
                    )
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
