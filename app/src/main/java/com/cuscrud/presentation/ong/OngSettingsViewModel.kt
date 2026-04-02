package com.cuscrud.presentation.ong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.UpdateOngInteractor
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.repository.canManageInventory
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OngSettingsViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val updateOngInteractor: UpdateOngInteractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(OngSettingsUiState())
    val uiState: StateFlow<OngSettingsUiState> = _uiState.asStateFlow()

    init {
        observeActiveInventory()
    }

    private fun observeActiveInventory() {
        combine(
            repository.activeInventoryId,
            repository.activeInventoryRole
        ) { id, role ->
            id to role
        }.onEach { (id, role) ->
            if (id != null) {
                // Em um cenário real, buscaríamos o nome da ONG do cache ou API aqui
                // Por simplificação, vamos assumir que o repositório ou um GetOngByIdInteractor proveria isso
                _uiState.update { it.copy(ongId = id, userRole = role) }
                loadOngDetails(id)
            }
        }.launchIn(viewModelScope)
    }

    private fun loadOngDetails(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.getInventories()
            if (result is Result.Success) {
                val currentOng = result.data.find { it.invId == id }
                _uiState.update { 
                    it.copy(
                        ongName = currentOng?.invName ?: "",
                        isLoading = false 
                    ) 
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onToggleEdit() {
        if (!_uiState.value.userRole.canManageInventory()) {
            _uiState.update { it.copy(userMessage = "Apenas o dono pode realizar esta ação.") }
            return
        }
        _uiState.update { 
            it.copy(
                isEditing = !it.isEditing,
                editName = it.ongName
            ) 
        }
    }

    fun onEditNameChanged(newName: String) {
        _uiState.update { it.copy(editName = newName) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = updateOngInteractor(state.ongId, state.editName)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            ongName = result.data.invName,
                            isEditing = false,
                            isLoading = false,
                            userMessage = "Nome da ONG atualizado com sucesso!",
                            isSuccess = true
                        ) 
                    }
                }
                is Result.Error -> {
                    val message = if (result.exception is java.io.IOException) {
                        "Não foi possível comunicar com o servidor. Tente novamente mais tarde."
                    } else {
                        result.exception.message ?: "Erro ao atualizar ONG."
                    }
                    _uiState.update { it.copy(isLoading = false, userMessage = message) }
                }
                else -> {}
            }
        }
    }

    fun snackbarMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
