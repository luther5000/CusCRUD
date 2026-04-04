package com.cuscrud.presentation.ong

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.domain.interactor.GetOngsInteractor
import com.cuscrud.domain.interactor.SetActiveOngInteractor
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsável pela lógica de seleção e alternância entre ONGs.
 */
@HiltViewModel
class SelectOngViewModel @Inject constructor(
    private val getOngsInteractor: GetOngsInteractor,
    private val setActiveOngInteractor: SetActiveOngInteractor,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectOngUiState())
    val uiState: StateFlow<SelectOngUiState> = _uiState.asStateFlow()

    init {
        loadOngs()
        observeSavedStateMessages()
    }

    private fun observeSavedStateMessages() {
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String?>("success_message", null).collect { message ->
                if (message != null) {
                    _uiState.update { 
                        it.copy(
                            userMessage = message, 
                            isOngSelected = false 
                        ) 
                    }
                    // Força o recarregamento da lista para refletir a remoção
                    loadOngs()
                }
            }
        }
    }

    /**
     * Busca a lista de ONGs as quais o usuário possui acesso.
     */
    fun loadOngs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getOngsInteractor()) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(ongs = result.data, isLoading = false) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            userMessage = result.exception.message ?: "Não foi possível carregar as ONGs.", 
                            isLoading = false 
                        ) 
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Define uma ONG específica como ativa e sinaliza para a navegação prosseguir para o inventário.
     */
    fun onOngSelected(ong: InventoryDto) {
        val role = ong.role?.let { Role.fromInt(it) }
        
        if (role == null) {
            _uiState.update { it.copy(userMessage = "Seu nível de acesso nesta ONG é inválido.") }
            return
        }

        viewModelScope.launch {
            try {
                setActiveOngInteractor(ong.invId, role)
                _uiState.update { it.copy(isOngSelected = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Não foi possível carregar o inventário da ONG selecionada.") }
            }
        }
    }

    /**
     * Chamado após a navegação ser concluída para resetar o estado de gatilho.
     */
    fun onNavigated() {
        _uiState.update { it.copy(isOngSelected = false) }
    }

    /**
     * Limpa a mensagem do Snackbar após ser exibida e limpa a origem no SavedStateHandle.
     */
    fun snackbarMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
        savedStateHandle["success_message"] = null
    }
}
