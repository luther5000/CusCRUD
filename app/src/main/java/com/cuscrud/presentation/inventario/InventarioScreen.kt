package com.cuscrud.presentation.inventario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    viewModel: InventarioViewModel,
    onTipoSelected: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventário Geral") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { /* Implementar reload se necessário */ }) {
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum produto no inventário.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            inventario.forEach { (tipo, produtos) ->
                item {
                    TipoHeader(tipo = tipo, quantidadeProdutos = produtos.size) {
                        onTipoClick(tipo.id)
                    }
                }
                items(produtos) { produto ->
                    ProdutoSimpleItem(produto = produto)
                }
            }
        }
    }
}

@Composable
fun TipoHeader(tipo: Tipo, quantidadeProdutos: Int, onClick: () -> Unit) {
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
                Text(
                    text = "$quantidadeProdutos produto(s)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Default.Inventory, contentDescription = null)
        }
    }
}

@Composable
fun ProdutoSimpleItem(produto: Produto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = produto.marca, fontWeight = FontWeight.Medium)
                Text(
                    text = "Qtd: ${produto.quantidade} ${produto.unidadeMedida}",
                    fontSize = 12.sp
                )
            }
        }
    }
}