package com.cuscrud.presentation.ong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.AddColaboradorInteractor
import com.cuscrud.domain.interactor.DeleteOngInteractor
import com.cuscrud.domain.interactor.GetColaboradoresInteractor
import com.cuscrud.domain.interactor.UpdateOngInteractor
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.repository.canManageInventory
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsável por gerenciar as definições da ONG e a gestão de colaboradores.
 * Centraliza as operações de edição, remoção e controle de acesso (RBAC).
 */
@HiltViewModel
class OngSettingsViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val updateOngInteractor: UpdateOngInteractor,
    private val deleteOngInteractor: DeleteOngInteractor,
    private val getColaboradoresInteractor: GetColaboradoresInteractor,
    private val addColaboradorInteractor: AddColaboradorInteractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(OngSettingsUiState())
    val uiState: StateFlow<OngSettingsUiState> = _uiState.asStateFlow()

    init {
        observeActiveInventory()
    }

    /**
     * Observa o inventário ativo e carrega os detalhes e colaboradores automaticamente.
     */
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
                if (role.canManageInventory()) {
                    loadColaboradores()
                }
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

    private fun loadColaboradores() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingColaboradores = true) }
            when (val result = getColaboradoresInteractor()) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            colaboradores = result.data,
                            isLoadingColaboradores = false 
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingColaboradores = false) }
                }
                else -> {}
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

    // region Remoção de ONG
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
    // endregion

    // region Gestão de Colaboradores
    fun onShowAddColaboradorClick() {
        _uiState.update { it.copy(showAddColaboradorDialog = true) }
    }

    fun onDismissAddColaborador() {
        _uiState.update { 
            it.copy(
                showAddColaboradorDialog = false,
                addColaboradorEmail = "",
                addColaboradorRole = Role.EDITOR
            ) 
        }
    }

    fun onAddColaboradorEmailChanged(email: String) {
        _uiState.update { it.copy(addColaboradorEmail = email) }
    }

    fun onAddColaboradorRoleChanged(role: Role) {
        _uiState.update { it.copy(addColaboradorRole = role) }
    }

    fun onConfirmAddColaborador() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingColaborador = true) }
            when (val result = addColaboradorInteractor(state.addColaboradorEmail, state.addColaboradorRole)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isAddingColaborador = false,
                            showAddColaboradorDialog = false,
                            addColaboradorEmail = "",
                            addColaboradorRole = Role.EDITOR,
                            userMessage = "Colaborador adicionado com sucesso!"
                        ) 
                    }
                    loadColaboradores()
                }
                is Result.Error -> {
                    val message = when {
                        result.exception is java.io.IOException -> 
                            "Não foi possível comunicar com o servidor. Tente novamente mais tarde."
                        result.exception.message?.contains("not found", ignoreCase = true) == true ->
                            "Usuário não encontrado no sistema."
                        result.exception.message?.contains("already", ignoreCase = true) == true ->
                            "Este usuário já faz parte desta equipe."
                        else -> result.exception.message ?: "Erro ao adicionar colaborador."
                    }
                    _uiState.update { it.copy(isAddingColaborador = false, userMessage = message) }
                }
                else -> {}
            }
        }
    }
    // endregion

    fun snackbarMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
