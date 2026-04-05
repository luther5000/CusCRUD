package com.cuscrud.presentation.produtos

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cuscrud.domain.repository.canEditProducts
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProdutoScreen(
    viewModel: AddProdutoViewModel,
    onBackClick: (String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    var showExitConfirmation by remember { mutableStateOf(false) }
    var showAddTipoDialog by remember { mutableStateOf(false) }
    
    // RBAC: Verifica se o usuário tem permissão para editar/adicionar
    val canEdit = uiState.userRole.canEditProducts()

    BackHandler {
        if (canEdit) showExitConfirmation = true else onBackClick(null)
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            if (!uiState.isProductSaved) {
                snackbarHostState.showSnackbar(it)
                viewModel.snackbarMessageShown()
            }
        }
    }

    LaunchedEffect(uiState.isProductSaved) {
        if (uiState.isProductSaved) {
            onBackClick(uiState.userMessage)
        }
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text(if (uiState.isEditMode) "Descartar alterações?" else "Descartar adição?") },
            text = { Text("Se você sair agora, todas as informações inseridas serão perdidas.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    onBackClick(null)
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) { Text("Continuar editando") }
            }
        )
    }

    if (showAddTipoDialog) {
        var novoTipoNome by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTipoDialog = false },
            title = { Text("Nova Categoria") },
            text = {
                Column {
                    Text("Informe o nome da nova categoria de produtos.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = novoTipoNome,
                        onValueChange = { novoTipoNome = it },
                        label = { Text("Nome da Categoria") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAddNovoTipo(novoTipoNome)
                    showAddTipoDialog = false
                }) { Text("Criar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTipoDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Editar Produto" else "Adicionar Produto") },
                navigationIcon = {
                    IconButton(onClick = { if (canEdit) showExitConfirmation = true else onBackClick(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Seleção de Tipo
                var expandedTipo by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedTipo && canEdit,
                    onExpandedChange = { if (canEdit) expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = uiState.tipoSelecionado?.nome ?: "Selecione o Tipo",
                        onValueChange = {},
                        readOnly = true,
                        enabled = canEdit,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo && canEdit,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        uiState.tipos.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo.nome) },
                                onClick = {
                                    viewModel.onTipoSelected(tipo)
                                    expandedTipo = false
                                }
                            )
                        }
                        Divider()
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Adicionar Nova Categoria") 
                                }
                            },
                            onClick = {
                                showAddTipoDialog = true
                                expandedTipo = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.marca,
                    onValueChange = { viewModel.onMarcaChanged(it) },
                    label = { Text("Marca/Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canEdit
                )

                // Data de Validade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canEdit) {
                            val calendar = Calendar.getInstance()
                            calendar.time = uiState.dataValidade
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedDate = Calendar.getInstance()
                                    selectedDate.set(year, month, dayOfMonth)
                                    viewModel.onDataValidadeChanged(selectedDate.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    OutlinedTextField(
                        value = dateFormatter.format(uiState.dataValidade),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Data de Validade") },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.unidade,
                        onValueChange = { viewModel.onUnidadeChanged(it) },
                        label = { Text("Valor Unidade") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        enabled = canEdit
                    )

                    var expandedMedida by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedMedida && canEdit,
                        onExpandedChange = { if (canEdit) expandedMedida = !expandedMedida },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = uiState.unidadeMedida,
                            onValueChange = {},
                            readOnly = true,
                            enabled = canEdit,
                            label = { Text("Medida") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMedida) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMedida && canEdit,
                            onDismissRequest = { expandedMedida = false }
                        ) {
                            uiState.unidadesMedida.forEach { medida ->
                                DropdownMenuItem(
                                    text = { Text(medida) },
                                    onClick = {
                                        viewModel.onUnidadeMedidaChanged(medida)
                                        expandedMedida = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.quantidade,
                    onValueChange = { viewModel.onQuantidadeChanged(it) },
                    label = { Text("Quantidade no Inventário") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canEdit
                )

                Spacer(modifier = Modifier.weight(1f))

                if (canEdit) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { showExitConfirmation = true }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = { viewModel.onSaveProduto() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            else Text(if (uiState.isEditMode) "Confirmar" else "Adicionar")
                        }
                    }
                }
            }
            
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}
