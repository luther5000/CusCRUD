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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.canEditProducts
import com.cuscrud.ui.components.CurvedHeader
import com.cuscrud.ui.components.TactileButton

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
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.fetchInventario()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val canAdd = when (val state = uiState) {
        is InventarioUiState.Success -> state.userRole.canEditProducts()
        else -> false
    }

    val successMessage by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("success_message", null)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }

    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("success_message")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box {
                CurvedHeader(
                    title = "Inventário",
                    subtitle = "Gerencie os itens da sua ONG"
                )
                
                // Botões de navegação sobre o Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onChangeOngClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val state = uiState) {
                    is InventarioUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag("loading_indicator")
                        )
                    }
                    is InventarioUiState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            TactileButton(
                                text = "Tentar Novamente",
                                onClick = { viewModel.fetchInventario() },
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    }
                    is InventarioUiState.Success -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            InventarioList(
                                inventario = state.inventario,
                                onTipoClick = onTipoSelected,
                                modifier = Modifier.weight(1f)
                            )
                            
                            if (canAdd) {
                                TactileButton(
                                    text = "Adicionar Produto",
                                    onClick = onAddProdutoClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventarioList(
    inventario: Map<Tipo, List<Produto>>,
    onTipoClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (inventario.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Você não possui produtos salvos nesta ONG.\nAdicione um clicando no botão abaixo.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tipo.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalQuantidade $unidadeMedida em estoque",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$quantidadeLotes lote(s) cadastrado(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.Inventory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
