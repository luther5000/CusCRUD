package com.cuscrud.presentation.produtos

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.cuscrud.ui.components.CurvedHeader
import com.cuscrud.ui.components.ModernInput
import com.cuscrud.ui.components.TactileButton
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box {
                CurvedHeader(
                    title = if (uiState.isEditMode) "Editar" else "Adicionar",
                    subtitle = if (uiState.isEditMode) "Atualize os dados do item" else "Cadastre um novo item"
                )
                
                IconButton(
                    onClick = { if (canEdit) showExitConfirmation = true else onBackClick(null) },
                    modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Seleção de Tipo
                var expandedTipo by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedTipo && canEdit,
                    onExpandedChange = { if (canEdit) expandedTipo = !expandedTipo }
                ) {
                    ModernInput(
                        value = uiState.tipoSelecionado?.nome ?: "Selecione a Categoria",
                        onValueChange = {},
                        label = "Categoria",
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) }
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
                                    Text("Nova Categoria") 
                                }
                            },
                            onClick = {
                                showAddTipoDialog = true
                                expandedTipo = false
                            }
                        )
                    }
                }

                ModernInput(
                    value = uiState.marca,
                    onValueChange = { viewModel.onMarcaChanged(it) },
                    label = "Marca/Nome",
                    placeholder = "Ex: Arroz Tio João",
                    modifier = Modifier.fillMaxWidth()
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
                    ModernInput(
                        value = dateFormatter.format(uiState.dataValidade),
                        onValueChange = {},
                        label = "Data de Validade",
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ModernInput(
                        value = uiState.unidade,
                        onValueChange = { viewModel.onUnidadeChanged(it) },
                        label = "Peso/Valor",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    var expandedMedida by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedMedida && canEdit,
                        onExpandedChange = { if (canEdit) expandedMedida = !expandedMedida },
                        modifier = Modifier.weight(1f)
                    ) {
                        ModernInput(
                            value = uiState.unidadeMedida,
                            onValueChange = {},
                            label = "Medida",
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMedida) }
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

                ModernInput(
                    value = uiState.quantidade,
                    onValueChange = { viewModel.onQuantidadeChanged(it) },
                    label = "Qtd. Total",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (canEdit) {
                    TactileButton(
                        text = if (uiState.isEditMode) "Salvar Alterações" else "Cadastrar Produto",
                        onClick = { viewModel.onSaveProduto() },
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = uiState.isLoading
                    )
                    
                    TactileButton(
                        text = "Cancelar",
                        onClick = { showExitConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = false
                    )
                }
            }
        }
    }
}
