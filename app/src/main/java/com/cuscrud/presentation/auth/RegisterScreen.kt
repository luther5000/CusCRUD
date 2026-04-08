package com.cuscrud.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cuscrud.ui.components.CurvedHeader
import com.cuscrud.ui.components.ModernInput
import com.cuscrud.ui.components.TactileButton
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is RegisterViewModel.RegisterUiEvent.RegisterSuccess -> onRegisterSuccess()
                is RegisterViewModel.RegisterUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurvedHeader(
                title = "Criar Conta",
                subtitle = "Preencha os dados abaixo"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ModernInput(
                    value = viewModel.name,
                    onValueChange = viewModel::onNameChanged,
                    label = "Nome Completo",
                    placeholder = "Seu Nome",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                ModernInput(
                    value = viewModel.email,
                    onValueChange = viewModel::onEmailChanged,
                    label = "E-mail",
                    placeholder = "seu@email.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                ModernInput(
                    value = viewModel.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = "Senha",
                    placeholder = "Min. 8 caracteres",
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )

                ModernInput(
                    value = viewModel.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChanged,
                    label = "Confirmar Senha",
                    placeholder = "Repita a senha",
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                TactileButton(
                    text = "Registrar Agora",
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onRegisterClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = viewModel.isLoading
                )

                TactileButton(
                    text = "Voltar para o login",
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth(),
                    isPrimary = false
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
