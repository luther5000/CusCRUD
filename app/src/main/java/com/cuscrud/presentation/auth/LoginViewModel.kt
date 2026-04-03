package com.cuscrud.presentation.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.data.remote.dto.LoginRequest
import com.cuscrud.domain.repository.AuthRepository
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val _uiEvent = Channel<LoginUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEmailChanged(newValue: String) {
        email = newValue
    }

    fun onPasswordChanged(newValue: String) {
        password = newValue
    }

    fun onLoginClick() {
        if (email.isBlank() || password.isBlank()) {
            viewModelScope.launch {
                _uiEvent.send(LoginUiEvent.ShowError("É necessário preencher todos os campos"))
            }
            return
        }

        viewModelScope.launch {
            isLoading = true
            val result = authRepository.login(LoginRequest(login = email, passwd = password))
            isLoading = false

            when (result) {
                is Result.Success -> {
                    _uiEvent.send(LoginUiEvent.LoginSuccess)
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Ocorreu um erro inesperado."
                    _uiEvent.send(LoginUiEvent.ShowError(message))
                }
                is Result.Loading -> { /* Já tratado pelo isLoading */ }
            }
        }
    }

    sealed class LoginUiEvent {
        object LoginSuccess : LoginUiEvent()
        data class ShowError(val message: String) : LoginUiEvent()
    }
}
