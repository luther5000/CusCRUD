package com.cuscrud.presentation.detalhes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuscrud.domain.model.Produto
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdutoDetalhesScreen(
    viewModel: ProdutoDetalhesViewModel,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Produto") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is ProdutoDetalhesUiState.Success) {
                (uiState as ProdutoDetalhesUiState.Success).produto?.let { produto ->
                    FloatingActionButton(onClick = { onEditClick(produto.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Produto")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is ProdutoDetalhesUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProdutoDetalhesUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ProdutoDetalhesUiState.Success -> {
                    if (state.produto == null) {
                        Text(
                            text = "Produto não encontrado.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        ProdutoInfo(produto = state.produto)
                    }
                }
            }
        }
    }
}

@Composable
fun ProdutoInfo(produto: Produto) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfoCard(
            icon = Icons.Default.Store,
            label = "Marca",
            value = produto.marca
        )
        InfoCard(
            icon = Icons.Default.Category,
            label = "Tipo",
            value = produto.tipo.nome
        )
        InfoCard(
            icon = Icons.Default.Numbers,
            label = "Quantidade",
            value = "${produto.quantidade} ${produto.unidadeMedida}"
        )
        InfoCard(
            icon = Icons.Default.CalendarMonth,
            label = "Data de Validade",
            value = dateFormatter.format(produto.dataValidade)
        )
    }
}

@Composable
fun InfoCard(icon: ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
