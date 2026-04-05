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

    // Flag para controlar carregamento silencioso (evita flicker)
    private var isFetchingSilently = false

    init {
        observeRole()
    }

    private fun observeRole() {
        viewModelScope.launch {
            inventoryRepository.activeInventoryRole.collect { role ->
                _uiState.update { state ->
                    if (state is InventarioUiState.Success) state.copy(userRole = role) else state
                }
            }
        }
    }

    fun fetchInventario() {
        if (isFetchingSilently) return
        
        viewModelScope.launch {
            val currentState = _uiState.value
            val currentRole = (currentState as? InventarioUiState.Success)?.userRole ?: inventoryRepository.activeInventoryRole.value
            
            // Se já temos dados, fazemos um carregamento "silencioso" (não mudamos para Loading)
            val shouldShowFullLoading = currentState !is InventarioUiState.Success
            
            if (shouldShowFullLoading) {
                _uiState.value = InventarioUiState.Loading
            } else {
                isFetchingSilently = true
            }
            
            when (val result = getInventarioAgrupadoInteractor()) {
                is Result.Success -> {
                    _uiState.value = InventarioUiState.Success(
                        inventario = result.data,
                        userRole = currentRole
                    )
                }
                is Result.Error -> {
                    // Só mostramos erro total se não houver dados anteriores
                    if (shouldShowFullLoading) {
                        _uiState.value = InventarioUiState.Error(
                            result.exception.message ?: "Erro ao carregar o inventário."
                        )
                    }
                }
                else -> {}
            }
            isFetchingSilently = false
        }
    }
}
