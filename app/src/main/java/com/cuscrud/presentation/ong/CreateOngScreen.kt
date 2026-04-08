package com.cuscrud.presentation.ong

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cuscrud.ui.components.CurvedHeader
import com.cuscrud.ui.components.ModernInput
import com.cuscrud.ui.components.TactileButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOngScreen(
    viewModel: CreateOngViewModel,
    onBackClick: () -> Unit,
    onOngCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessageShown()
        }
    }

    LaunchedEffect(uiState.isOngCreated) {
        if (uiState.isOngCreated) {
            onOngCreated()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurvedHeader(
                title = "Nova ONG",
                subtitle = "Crie um novo espaço de trabalho"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Dê um nome para a sua nova ONG. Isso criará um inventário exclusivo para ela.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                ModernInput(
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label = "Nome da Organização",
                    placeholder = "Ex: SOS Mata Atlântica"
                )

                Spacer(modifier = Modifier.weight(1f))

                TactileButton(
                    text = "Criar Organização",
                    onClick = { viewModel.onCreateClick() },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState.isLoading
                )

                TactileButton(
                    text = "Voltar",
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth(),
                    isPrimary = false
                )
            }
        }
    }
}
