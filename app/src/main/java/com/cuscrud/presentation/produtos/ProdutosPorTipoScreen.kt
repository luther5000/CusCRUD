package com.cuscrud.presentation.produtos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cuscrud.domain.model.Produto
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdutosPorTipoScreen(
    viewModel: ProdutosPorTipoViewModel,
    navController: NavController,
    onBackClick: () -> Unit,
    onProdutoClick: (Long) -> Unit,
    onAddProdutoClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    // Exibe snackbars de erro ou sucesso local
    LaunchedEffect(uiState.errorMessage, uiState.mensagemSucesso) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limparMensagens()
        }
        uiState.mensagemSucesso?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limparMensagens()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produtos da Categoria") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProdutoClick) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
            }
        }
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
                    text = "Nenhum produto encontrado nesta categoria.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.produtos, key = { it.id }) { produto ->
                        ProdutoListItem(
                            produto = produto,
                            onClick = { onProdutoClick(produto.id) },
                            onDeleteClick = { viewModel.solicitarRemocao(produto) }
                        )
                    }
                }
            }

            if (uiState.isLoading && uiState.produtos.isNotEmpty()) {
                 LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }

        // Diálogo de Confirmação (Double-check)
        uiState.produtoParaRemover?.let { produto ->
            AlertDialog(
                onDismissRequest = { viewModel.cancelarRemocao() },
                title = { Text("Confirmar Remoção") },
                text = { Text("Deseja realmente remover ${produto.marca}?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmarRemocao() }) {
                        Text("Sim")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelarRemocao() }) {
                        Text("Não")
                    }
                }
            )
        }
    }
}

@Composable
fun ProdutoListItem(
    produto: Produto, 
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = produto.marca,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Qtd: ${produto.quantidade} (${produto.unidade} ${produto.unidadeMedida})")
                    Text(
                        text = "Validade: ${dateFormatter.format(produto.dataValidade)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remover Produto",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
