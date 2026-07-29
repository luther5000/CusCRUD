package com.cuscrud.presentation.ong

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.domain.model.Role
import com.cuscrud.ui.components.CurvedHeader
import com.cuscrud.ui.components.TactileButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectOngScreen(
    viewModel: SelectOngViewModel,
    onOngSelected: () -> Unit,
    onCreateOngClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessageShown()
        }
    }

    LaunchedEffect(uiState.isOngSelected) {
        if (uiState.isOngSelected) {
            onOngSelected()
            viewModel.onNavigated() // Reseta o estado após disparar a navegação
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
            CurvedHeader(
                title = "Suas ONGs",
                subtitle = "Selecione uma organização para gerenciar"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.ongs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Você não possui acesso a nenhuma ONG.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TactileButton(
                            text = "CRIAR MINHA PRIMEIRA ONG",
                            onClick = onCreateOngClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.ongs) { ong ->
                                OngItem(
                                    ong = ong,
                                    onClick = { viewModel.onOngSelected(ong) }
                                )
                            }
                        }
                        
                        TactileButton(
                            text = "Nova ONG",
                            onClick = onCreateOngClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            isPrimary = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OngItem(
    ong: InventoryDto,
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
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = ong.invName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Nível de acesso: ${ong.role?.let { Role.fromInt(it) }?.name ?: "Desconhecido"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
