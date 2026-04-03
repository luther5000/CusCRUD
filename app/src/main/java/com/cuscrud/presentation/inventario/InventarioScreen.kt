package com.cuscrud.presentation.inventario

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    viewModel: InventarioViewModel,
    navController: NavController,
    onTipoSelected: (Long) -> Unit,
    onAddProdutoClick: () -> Unit,
    onAddSampleData: () -> Unit,
    onChangeOngClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observa mensagens de sucesso vindas de outras telas através do savedStateHandle
    val successMessage by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("success_message", null)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }

    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            // Limpa a mensagem para evitar que ela apareça novamente ao recompor ou voltar
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("success_message")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventário Geral") },
                navigationIcon = {
                    // Botão para retornar à seleção de ONGs
                    IconButton(onClick = onChangeOngClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar para ONGs"
                        )
                    }
                },
                actions = {
                    // Botão para definições da ONG
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações da ONG"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProdutoClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is InventarioUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is InventarioUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.fetchInventario() }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
                is InventarioUiState.Success -> {
                    InventarioList(
                        inventario = state.inventario,
                        onTipoClick = onTipoSelected
                    )
                }
            }
        }
    }
}

@Composable
fun InventarioList(
    inventario: Map<Tipo, List<Produto>>,
    onTipoClick: (Long) -> Unit
) {
    if (inventario.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Você não possui produtos salvos nesta ONG.\nAdicione um clicando no botão '+'.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            inventario.forEach { (tipo, produtos) ->
                val totalQuantidade = produtos.sumOf { it.quantidade }
                val unidadeMedida = produtos.firstOrNull()?.unidadeMedida ?: ""

                item {
                    TipoSummaryItem(
                        tipo = tipo,
                        totalQuantidade = totalQuantidade,
                        unidadeMedida = unidadeMedida,
                        quantidadeLotes = produtos.size
                    ) {
                        onTipoClick(tipo.id)
                    }
                }
            }
        }
    }
}

@Composable
fun TipoSummaryItem(
    tipo: Tipo,
    totalQuantidade: Long,
    unidadeMedida: String,
    quantidadeLotes: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = tipo.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total em estoque: $totalQuantidade $unidadeMedida",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$quantidadeLotes produtos(s) cadastrado(s)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.Default.Inventory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
