package com.cuscrud.presentation.produtos

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProdutoScreen(
    viewModel: AddProdutoViewModel,
    onBackClick: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    var showExitConfirmation by remember { mutableStateOf(false) }

    // Trata o botão de voltar do sistema
    BackHandler {
        showExitConfirmation = true
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            // Só exibe o snackbar se NÃO for sucesso (erros ou validações)
            if (!uiState.isProductSaved) {
                snackbarHostState.showSnackbar(it)
                viewModel.snackbarMessageShown()
            }
        }
    }

    LaunchedEffect(uiState.isProductSaved) {
        if (uiState.isProductSaved) {
            // Navega de volta indicando sucesso
            onBackClick(true)
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
                    onBackClick(false)
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Continuar editando")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Editar Produto" else "Adicionar Produto") },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirmation = true }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Seleção de Tipo
            var expandedTipo by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedTipo,
                onExpandedChange = { expandedTipo = !expandedTipo }
            ) {
                OutlinedTextField(
                    value = uiState.tipoSelecionado?.nome ?: "Selecione o Tipo",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedTipo,
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
                }
            }

            OutlinedTextField(
                value = uiState.marca,
                onValueChange = { viewModel.onMarcaChanged(it) },
                label = { Text("Marca/Nome") },
                modifier = Modifier.fillMaxWidth()
            )

            // Data de Validade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
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
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Selecionar Data")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.unidade,
                    onValueChange = { viewModel.onUnidadeChanged(it) },
                    label = { Text("Valor Unidade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                // Seleção de Unidade de Medida
                var expandedMedida by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedMedida,
                    onExpandedChange = { expandedMedida = !expandedMedida },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.unidadeMedida,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Medida") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMedida) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMedida,
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showExitConfirmation = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { viewModel.onSaveProduto() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (uiState.isEditMode) "Confirmar" else "Adicionar")
                    }
                }
            }
        }
    }
}
