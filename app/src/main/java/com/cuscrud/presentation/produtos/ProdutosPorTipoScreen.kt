package com.cuscrud.presentation.produtos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.canEditProducts
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdutosPorTipoScreen(
    viewModel: ProdutosPorTipoViewModel,
    navController: NavController,
    onBackClick: () -> Unit,
    onAddProdutoClick: () -> Unit,
    onProdutoClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Força o refresh sempre que a tela volta ao primeiro plano (RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadProdutos()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Observa mensagens de sucesso vindas do AddProdutoScreen
    val successMessage by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("success_message", null)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }

    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            viewModel.loadProdutos()
            snackbarHostState.showSnackbar(message)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("success_message")
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limparMensagens()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produtos") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.userRole.canEditProducts()) {
                FloatingActionButton(onClick = onAddProdutoClick) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.produtos.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.produtos.isEmpty()) {
                Text(
                    text = "Nenhum produto cadastrado nesta categoria.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.produtos) { produto ->
                        ProdutoItem(
                            produto = produto,
                            canEdit = uiState.userRole.canEditProducts(),
                            dateFormatter = dateFormatter,
                            onClick = { onProdutoClick(produto.id) },
                            onAumentarQuantidade = { viewModel.alterarQuantidade(produto, 1) },
                            onDiminuirQuantidade = { viewModel.alterarQuantidade(produto, -1) }
                        )
                    }
                }
                
                if (uiState.isLoading && uiState.produtos.isNotEmpty()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun ProdutoItem(
    produto: Produto,
    canEdit: Boolean,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    onAumentarQuantidade: () -> Unit,
    onDiminuirQuantidade: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = produto.marca,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Vencimento: ${dateFormatter.format(produto.dataValidade)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${produto.unidade} ${produto.unidadeMedida}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (canEdit) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDiminuirQuantidade) {
                        Icon(Icons.Default.Remove, contentDescription = "Diminuir")
                    }
                    Text(
                        text = produto.quantidade.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = onAumentarQuantidade) {
                        Icon(Icons.Default.Add, contentDescription = "Aumentar")
                    }
                }
            } else {
                Text(
                    text = "Qtd: ${produto.quantidade}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
