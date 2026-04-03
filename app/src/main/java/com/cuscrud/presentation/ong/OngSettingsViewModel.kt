package com.cuscrud.presentation.ong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.DeleteOngInteractor
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
    private val updateOngInteractor: UpdateOngInteractor,
    private val deleteOngInteractor: DeleteOngInteractor
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
                            userMessage = "Nome da ONG atualizado com sucesso!"
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

    fun onDeleteClick() {
        if (!_uiState.value.userRole.canManageInventory()) {
            _uiState.update { it.copy(userMessage = "Apenas o dono pode realizar esta ação.") }
            return
        }
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun onCancelDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun onConfirmDelete() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteConfirmation = false) }
            when (val result = deleteOngInteractor(state.ongId)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            userMessage = "ONG removida com sucesso!"
                        ) 
                    }
                }
                is Result.Error -> {
                    val message = if (result.exception is java.io.IOException) {
                        "Não foi possível comunicar com o servidor. Tente novamente mais tarde."
                    } else {
                        result.exception.message ?: "Erro ao remover ONG."
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
