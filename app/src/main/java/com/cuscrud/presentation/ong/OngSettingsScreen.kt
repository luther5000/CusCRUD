package com.cuscrud.presentation.ong

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuscrud.domain.repository.canManageInventory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OngSettingsScreen(
    viewModel: OngSettingsViewModel,
    onBackClick: () -> Unit,
    onDeleteSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessageShown()
        }
    }

    // Redireciona ao selecionar ONG ou remover com sucesso
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onDeleteSuccess()
        }
    }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onCancelDelete() },
            title = { Text("Remover ONG") },
            text = { Text("Tem certeza que deseja remover permanentemente a ONG '${uiState.ongName}' e todo o seu inventário? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onConfirmDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onCancelDelete() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Definições da ONG") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (uiState.userRole.canManageInventory()) {
                        if (uiState.isEditing) {
                            IconButton(onClick = { viewModel.onSaveClick() }, enabled = !uiState.isLoading) {
                                Icon(Icons.Default.Save, contentDescription = "Salvar")
                            }
                        } else {
                            IconButton(onClick = { viewModel.onToggleEdit() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && !uiState.isEditing && !uiState.showDeleteConfirmation) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Informações Gerais",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (uiState.isEditing) {
                        OutlinedTextField(
                            value = uiState.editName,
                            onValueChange = { viewModel.onEditNameChanged(it) },
                            label = { Text("Nome da ONG") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !uiState.isLoading
                        )
                    } else {
                        InfoRow(label = "Nome:", value = uiState.ongName)
                        InfoRow(label = "ID da Organização:", value = uiState.ongId)
                        InfoRow(label = "Seu Papel:", value = uiState.userRole?.name ?: "N/A")
                    }

                    if (!uiState.userRole.canManageInventory()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Apenas o dono pode editar as informações desta ONG.",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    
                    if (uiState.isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.onToggleEdit() },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isLoading
                            ) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = { viewModel.onSaveClick() },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Salvar")
                                }
                            }
                        }
                    }

                    // Botão de remoção (Apenas para o Dono)
                    if (uiState.userRole.canManageInventory() && !uiState.isEditing) {
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = { viewModel.onDeleteClick() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Remover Organização")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
    }
}
