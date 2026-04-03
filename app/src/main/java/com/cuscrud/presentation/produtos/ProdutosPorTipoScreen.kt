package com.cuscrud.presentation.produtos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.canEditProducts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdutosPorTipoScreen(
    viewModel: ProdutosPorTipoViewModel,
    onBackClick: () -> Unit,
    onAddProdutoClick: () -> Unit,
    onEditProdutoClick: (Long) -> Unit,
    onProdutoClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limparMensagens()
        }
    }

    LaunchedEffect(uiState.mensagemSucesso) {
        uiState.mensagemSucesso?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limparMensagens()
        }
    }

    // Alerta de confirmação de remoção
    uiState.produtoParaRemover?.let { produto ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelarRemocao() },
            title = { Text("Excluir Produto") },
            text = { Text("Deseja realmente excluir o produto '${produto.marca}'?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmarRemocao() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarRemocao() }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produtos") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            // RBAC: Oculta o botão de adicionar se for apenas Visualizador
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
                            onClick = { onProdutoClick(produto.id) },
                            onEditClick = { onEditProdutoClick(produto.id) },
                            onDeleteClick = { viewModel.solicitarRemocao(produto) }
                        )
                    }
                }
                
                // Overlay de carregamento para ações de mutação (ex: remoção)
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
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                    text = "${produto.quantidade} ${produto.unidadeMedida}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // RBAC: Oculta botões de edição/remoção se for apenas Visualizador
            if (canEdit) {
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
