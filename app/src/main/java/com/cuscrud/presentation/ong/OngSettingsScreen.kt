package com.cuscrud.presentation.ong

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuscrud.domain.model.Role
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
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessageShown()
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onDeleteSuccess()
        }
    }

    // Diálogo de Remoção de ONG
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onCancelDelete() },
            title = { Text("Remover ONG") },
            text = { Text("Tem certeza que deseja remover permanentemente a ONG '${uiState.ongName}'? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onConfirmDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remover") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onCancelDelete() }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo de Adição de Colaborador
    if (uiState.showAddColaboradorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissAddColaborador() },
            title = { Text("Adicionar Colaborador") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.addColaboradorEmail,
                        onValueChange = { viewModel.onAddColaboradorEmailChanged(it) },
                        label = { Text("E-mail do usuário") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isAddingColaborador
                    )
                    Text("Papel de Acesso:", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = uiState.addColaboradorRole == Role.EDITOR,
                            onClick = { viewModel.onAddColaboradorRoleChanged(Role.EDITOR) },
                            enabled = !uiState.isAddingColaborador
                        )
                        Text("Editor")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(
                            selected = uiState.addColaboradorRole == Role.READER,
                            onClick = { viewModel.onAddColaboradorRoleChanged(Role.READER) },
                            enabled = !uiState.isAddingColaborador
                        )
                        Text("Visualizador")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        keyboardController?.hide()
                        viewModel.onConfirmAddColaborador() 
                    },
                    enabled = !uiState.isAddingColaborador && uiState.addColaboradorEmail.isNotBlank()
                ) {
                    if (uiState.isAddingColaborador) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissAddColaborador() }, enabled = !uiState.isAddingColaborador) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Edição de Papel de Colaborador
    if (uiState.showEditColaboradorDialog && uiState.selectedColaborador != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissEditColaborador() },
            title = { Text("Editar Permissão") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Alterar papel de ${uiState.selectedColaborador?.name}:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = uiState.editColaboradorRole == Role.EDITOR,
                            onClick = { viewModel.onEditColaboradorRoleChanged(Role.EDITOR) },
                            enabled = !uiState.isUpdatingColaborador
                        )
                        Text("Editor")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(
                            selected = uiState.editColaboradorRole == Role.READER,
                            onClick = { viewModel.onEditColaboradorRoleChanged(Role.READER) },
                            enabled = !uiState.isUpdatingColaborador
                        )
                        Text("Visualizador")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Botão para disparar a remoção
                    TextButton(
                        onClick = { viewModel.onRemoveColaboradorClick() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        enabled = !uiState.isUpdatingColaborador
                    ) {
                        Icon(Icons.Default.PersonRemove, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Remover Colaborador")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        keyboardController?.hide()
                        viewModel.onConfirmUpdateColaborador() 
                    },
                    enabled = !uiState.isUpdatingColaborador
                ) {
                    if (uiState.isUpdatingColaborador) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissEditColaborador() }, enabled = !uiState.isUpdatingColaborador) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Confirmação de Remoção de Colaborador
    if (uiState.showRemoveColaboradorConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onCancelRemoveColaborador() },
            title = { Text("Confirmar Remoção") },
            text = { Text("Deseja realmente remover o acesso de ${uiState.selectedColaborador?.name} a este inventário?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.onConfirmRemoveColaborador() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !uiState.isRemovingColaborador
                ) {
                    if (uiState.isRemovingColaborador) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Remover")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onCancelRemoveColaborador() }, enabled = !uiState.isRemovingColaborador) {
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
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
                actions = {
                    if (uiState.userRole.canManageInventory()) {
                        IconButton(onClick = { viewModel.onToggleEdit() }) {
                            Icon(if (uiState.isEditing) Icons.Default.Close else Icons.Default.Edit, "Editar")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Informações Gerais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (uiState.isEditing) {
                    OutlinedTextField(
                        value = uiState.editName,
                        onValueChange = { viewModel.onEditNameChanged(it) },
                        label = { Text("Nome da ONG") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        trailingIcon = {
                            IconButton(onClick = { 
                                keyboardController?.hide()
                                viewModel.onSaveClick() 
                            }) {
                                Icon(Icons.Default.Check, "Salvar", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                } else {
                    InfoRow(label = "Nome:", value = uiState.ongName)
                    InfoRow(label = "Seu Papel:", value = uiState.userRole?.name ?: "N/A")
                }
            }

            // Seção de Colaboradores (Apenas para Dono)
            if (uiState.userRole.canManageInventory()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Equipe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.onShowAddColaboradorClick() }) {
                            Icon(Icons.Default.PersonAdd, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Adicionar")
                        }
                    }
                }

                if (uiState.isLoadingColaboradores) {
                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                }

                items(uiState.colaboradores) { user ->
                    ListItem(
                        modifier = Modifier.clickable(
                            enabled = user.role != Role.OWNER.value,
                            onClick = { viewModel.onEditColaboradorClick(user) }
                        ),
                        headlineContent = { Text(user.name) },
                        supportingContent = { Text(user.login) },
                        trailingContent = {
                            Badge(containerColor = if (user.role == Role.OWNER.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary) {
                                Text(Role.fromInt(user.role)?.name ?: "N/A")
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }

            item {
                if (uiState.userRole.canManageInventory() && !uiState.isEditing) {
                    Spacer(Modifier.height(32.dp))
                    OutlinedButton(
                        onClick = { viewModel.onDeleteClick() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Remover Organização")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
