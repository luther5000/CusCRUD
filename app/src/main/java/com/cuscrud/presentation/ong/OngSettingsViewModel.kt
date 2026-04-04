package com.cuscrud.presentation.ong

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.data.remote.dto.UserAccessDto
import com.cuscrud.domain.interactor.*
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.repository.canManageInventory
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel responsável por gerenciar as definições da ONG e a gestão de colaboradores.
 */
@HiltViewModel
class OngSettingsViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val updateOngInteractor: UpdateOngInteractor,
    private val deleteOngInteractor: DeleteOngInteractor,
    private val getColaboradoresInteractor: GetColaboradoresInteractor,
    private val addColaboradorInteractor: AddColaboradorInteractor,
    private val updateColaboradorRoleInteractor: UpdateColaboradorRoleInteractor,
    private val removeColaboradorInteractor: RemoveColaboradorInteractor
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
                if (role.canManageInventory()) {
                    loadColaboradores()
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun loadOngDetails(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getInventories()) {
                is Result.Success -> {
                    val currentOng = result.data.find { it.invId == id }
                    _uiState.update { it.copy(ongName = currentOng?.invName ?: "", isLoading = false) }
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Ocorreu um erro inesperado."
                    _uiState.update { it.copy(isLoading = false, userMessage = message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun loadColaboradores() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingColaboradores = true) }
            when (val result = getColaboradoresInteractor()) {
                is Result.Success -> {
                    _uiState.update { it.copy(colaboradores = result.data, isLoadingColaboradores = false) }
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Ocorreu um erro inesperado."
                    _uiState.update { it.copy(isLoadingColaboradores = false, userMessage = message) }
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
        _uiState.update { it.copy(isEditing = !it.isEditing, editName = it.ongName) }
    }

    fun onEditNameChanged(newName: String) {
        _uiState.update { it.copy(editName = newName) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        val newName = state.editName.trim()

        // 1. Validação: Nome em branco
        if (newName.isBlank()) {
            _uiState.update { it.copy(userMessage = "O preenchimento do nome é obrigatório.") }
            return
        }

        // 2. Validação: Limite de caracteres (Spec 5.2.2)
        if (newName.length > 255) {
            _uiState.update { it.copy(userMessage = "O nome da ONG não pode ultrapassar 255 caracteres.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Nota: Adicionado try/catch para IOExceptions (Cenário de falha de conexão)
            try {
                when (val result = updateOngInteractor(state.ongId, newName)) {
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
                        val message = result.exception.message ?: "Ocorreu um erro inesperado."
                        _uiState.update { it.copy(isLoading = false, userMessage = message) }
                    }
                    else -> {}
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Não foi possível comunicar com o servidor. Tente novamente mais tarde."
                    )
                }
            }
        }
    }

    // region Gestão de Colaboradores (Adição, Edição e Remoção)
    
    fun onShowAddColaboradorClick() {
        _uiState.update { it.copy(showAddColaboradorDialog = true) }
    }

    fun onDismissAddColaborador() {
        _uiState.update { it.copy(showAddColaboradorDialog = false, addColaboradorEmail = "", addColaboradorRole = Role.EDITOR) }
    }

    fun onAddColaboradorEmailChanged(email: String) {
        _uiState.update { it.copy(addColaboradorEmail = email) }
    }

    fun onAddColaboradorRoleChanged(role: Role) {
        _uiState.update { it.copy(addColaboradorRole = role) }
    }

    fun onConfirmAddColaborador() {
        val email = _uiState.value.addColaboradorEmail.trim()
        val role = _uiState.value.addColaboradorRole

        // Validação local
        if (email.isBlank()) {
            _uiState.update { it.copy(userMessage = "O preenchimento do e-mail é obrigatório.") }
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.length > 255) {
            _uiState.update { it.copy(userMessage = "E-mail com formato inválido ou muito longo.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingColaborador = true) }
            when (val result = addColaboradorInteractor(email, role)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isAddingColaborador = false, showAddColaboradorDialog = false, addColaboradorEmail = "", userMessage = "Colaborador adicionado com sucesso!") }
                    loadColaboradores()
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Ocorreu um erro inesperado."
                    _uiState.update { it.copy(isAddingColaborador = false, userMessage = message) }
                }
                else -> {}
            }
        }
    }

    // Edição de Papel
    fun onEditColaboradorClick(user: UserAccessDto) {
        if (user.role == Role.OWNER.value) return
        _uiState.update { 
            it.copy(
                selectedColaborador = user,
                editColaboradorRole = Role.fromInt(user.role) ?: Role.READER,
                showEditColaboradorDialog = true
            ) 
        }
    }

    fun onDismissEditColaborador() {
        _uiState.update { it.copy(showEditColaboradorDialog = false, selectedColaborador = null) }
    }

    fun onEditColaboradorRoleChanged(role: Role) {
        _uiState.update { it.copy(editColaboradorRole = role) }
    }

    fun onConfirmUpdateColaborador() {
        val state = _uiState.value
        val userId = state.selectedColaborador?.userId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingColaborador = true) }
            when (val result = updateColaboradorRoleInteractor(userId, state.editColaboradorRole)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isUpdatingColaborador = false,
                            showEditColaboradorDialog = false,
                            selectedColaborador = null,
                            userMessage = "Permissão atualizada com sucesso!"
                        ) 
                    }
                    loadColaboradores()
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Ocorreu um erro inesperado."
                    _uiState.update { it.copy(isUpdatingColaborador = false, userMessage = message) }
                }
                else -> {}
            }
        }
    }

    // Remoção de Colaborador
    fun onRemoveColaboradorClick() {
        _uiState.update { it.copy(showRemoveColaboradorConfirmation = true) }
    }

    fun onCancelRemoveColaborador() {
        _uiState.update { it.copy(showRemoveColaboradorConfirmation = false) }
    }

    fun onConfirmRemoveColaborador() {
        val state = _uiState.value
        val userId = state.selectedColaborador?.userId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isRemovingColaborador = true, showRemoveColaboradorConfirmation = false) }
            when (val result = removeColaboradorInteractor(userId)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isRemovingColaborador = false,
                            showEditColaboradorDialog = false,
                            selectedColaborador = null,
                            userMessage = "Colaborador removido com sucesso!"
                        ) 
                    }
                    loadColaboradores()
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Ocorreu um erro inesperado."
                    _uiState.update { it.copy(isRemovingColaborador = false, userMessage = message) }
                }
                else -> {}
            }
        }
    }
    // endregion

    fun onDeleteClick() {
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
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, userMessage = "ONG removida com sucesso!") }
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Ocorreu um erro inesperado."
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
