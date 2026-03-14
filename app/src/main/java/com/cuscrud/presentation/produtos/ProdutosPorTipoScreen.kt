package com.cuscrud.presentation.produtos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
    onProdutoClick: (Int) -> Unit,
    onAddProdutoClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Escuta o sinal de sucesso usando StateFlow (Abordagem sem LiveData)
    val successAdded by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("product_added_success", false)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    LaunchedEffect(successAdded) {
        if (successAdded) {
            snackbarHostState.showSnackbar("Produto adicionado com sucesso")
            // Limpa o sinal
            navController.currentBackStackEntry?.savedStateHandle?.set("product_added_success", false)
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
            when (val state = uiState) {
                is ProdutosPorTipoUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProdutosPorTipoUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ProdutosPorTipoUiState.Success -> {
                    if (state.produtos.isEmpty()) {
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
                            items(state.produtos) { produto ->
                                ProdutoListItem(
                                    produto = produto,
                                    onClick = { onProdutoClick(produto.id) }
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
fun ProdutoListItem(produto: Produto, onClick: () -> Unit) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
    }
}
