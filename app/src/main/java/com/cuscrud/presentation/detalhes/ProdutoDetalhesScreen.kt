package com.cuscrud.presentation.detalhes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.canEditProducts
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessageShown()
        }
    }

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
            // RBAC: Oculta botão de editar se for apenas Visualizador
            if (uiState.userRole.canEditProducts()) {
                uiState.produto?.let { produto ->
                    FloatingActionButton(onClick = { onEditClick(produto.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Produto")
                    }
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
            if (uiState.isLoading && uiState.produto == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.produto == null && !uiState.isLoading) {
                Text(
                    text = "Produto não encontrado.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                uiState.produto?.let { produto ->
                    Column {
                        if (uiState.isLoading || uiState.isUpdatingQuantity) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        
                        ProdutoInfo(
                            produto = produto,
                            canEdit = uiState.userRole.canEditProducts(),
                            isUpdating = uiState.isUpdatingQuantity,
                            onUpdateQuantity = { viewModel.updateQuantidade(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProdutoInfo(
    produto: Produto,
    canEdit: Boolean,
    isUpdating: Boolean,
    onUpdateQuantity: (Long) -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfoCard(icon = Icons.Default.Store, label = "Marca", value = produto.marca)
        InfoCard(icon = Icons.Default.Category, label = "Tipo", value = produto.tipo.nome)
        
        // Card de Quantidade com ajuste rápido (Bloqueado por RBAC)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Numbers, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(text = "Estoque Atual", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${produto.quantidade} ${produto.unidadeMedida}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Controles de estoque rápido (Ocultos para Visualizadores)
                if (canEdit) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onUpdateQuantity(produto.quantidade - 1) },
                            enabled = !isUpdating && produto.quantidade > 0
                        ) {
                            Icon(Icons.Default.Remove, "Remover unidade")
                        }
                        IconButton(
                            onClick = { onUpdateQuantity(produto.quantidade + 1) },
                            enabled = !isUpdating
                        ) {
                            Icon(Icons.Default.Add, "Adicionar unidade")
                        }
                    }
                }
            }
        }

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
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelMedium)
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
