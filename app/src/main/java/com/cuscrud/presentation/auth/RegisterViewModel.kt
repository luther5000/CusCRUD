package com.cuscrud.presentation.auth

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.data.remote.dto.RegisterRequest
import com.cuscrud.domain.repository.AuthRepository
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var name by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val _uiEvent = Channel<RegisterUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onNameChanged(newValue: String) { name = newValue }
    fun onEmailChanged(newValue: String) { email = newValue }
    fun onPasswordChanged(newValue: String) { password = newValue }
    fun onConfirmPasswordChanged(newValue: String) { confirmPassword = newValue }

    fun onRegisterClick() {
        // Validação: Campos obrigatórios
        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            sendError("É necessário preencher todos os campos obrigatórios")
            return
        }

        // Validação: Formato de e-mail
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            sendError("E-mail com formato inválido. Use o padrão exemplo@dominio.com")
            return
        }

        // Validação: Senhas coincidem
        if (password != confirmPassword) {
            sendError("As senhas não coincidem")
            return
        }

        viewModelScope.launch {
            isLoading = true
            val result = authRepository.register(
                RegisterRequest(name = name, login = email, passwd = password)
            )
            isLoading = false

            when (result) {
                is Result.Success -> {
                    _uiEvent.send(RegisterUiEvent.RegisterSuccess)
                }
                is Result.Error -> {
                    val message = when {
                        result.exception.message?.contains("409") == true -> "Já existe uma conta associada a este e-mail"
                        else -> "Não foi possível comunicar com o servidor. Tente novamente mais tarde."
                    }
                    _uiEvent.send(RegisterUiEvent.ShowError(message))
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun sendError(message: String) {
        viewModelScope.launch {
            _uiEvent.send(RegisterUiEvent.ShowError(message))
        }
    }

    sealed class RegisterUiEvent {
        object RegisterSuccess : RegisterUiEvent()
        data class ShowError(val message: String) : RegisterUiEvent()
    }
}
