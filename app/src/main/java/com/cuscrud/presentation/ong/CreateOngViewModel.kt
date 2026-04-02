package com.cuscrud.presentation.ong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.CreateOngInteractor
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsável pela lógica de criação de uma nova ONG.
 */
@HiltViewModel
class CreateOngViewModel @Inject constructor(
    private val createOngInteractor: CreateOngInteractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOngUiState())
    val uiState: StateFlow<CreateOngUiState> = _uiState.asStateFlow()

    fun onNameChanged(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun onCreateClick() {
        val name = _uiState.value.name
        if (name.isBlank()) {
            _uiState.update { it.copy(userMessage = "O preenchimento do nome é obrigatório.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = createOngInteractor(name)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isOngCreated = true) }
                }
                is Result.Error -> {
                    val message = if (result.exception is java.io.IOException) {
                        "Não foi possível comunicar com o servidor. Tente novamente mais tarde."
                    } else {
                        result.exception.message ?: "Erro ao criar ONG."
                    }
                    _uiState.update { it.copy(isLoading = false, userMessage = message) }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun snackbarMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
