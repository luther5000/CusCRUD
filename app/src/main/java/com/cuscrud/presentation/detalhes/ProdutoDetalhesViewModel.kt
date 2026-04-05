package com.cuscrud.presentation.detalhes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.EditProdutoInteractor
import com.cuscrud.domain.interactor.GetProdutoDetalhesInteractor
import com.cuscrud.domain.interactor.RemoveProdutoInteractor
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProdutoDetalhesViewModel @Inject constructor(
    private val getProdutoDetalhesInteractor: GetProdutoDetalhesInteractor,
    private val editProdutoInteractor: EditProdutoInteractor,
    private val removeProdutoInteractor: RemoveProdutoInteractor,
    private val inventoryRepository: InventoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val produtoId: Long = checkNotNull(savedStateHandle["produtoId"])

    private val _uiState = MutableStateFlow(ProdutoDetalhesUiState())
    val uiState: StateFlow<ProdutoDetalhesUiState> = _uiState.asStateFlow()

    init {
        observeUserRole()
        loadProduto()
    }

    private fun observeUserRole() {
        inventoryRepository.activeInventoryRole
            .onEach { role ->
                _uiState.update { it.copy(userRole = role) }
            }
            .launchIn(viewModelScope)
    }

    fun loadProduto() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getProdutoDetalhesInteractor(produtoId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(produto = result.data, isLoading = false) }
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Erro ao carregar detalhes."
                    _uiState.update { it.copy(userMessage = message, isLoading = false) }
                }
                Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun updateQuantidade(novaQuantidade: Long) {
        val produtoAtual = _uiState.value.produto ?: return
        if (novaQuantidade < 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingQuantity = true) }
            val produtoAtualizado = produtoAtual.copy(quantidade = novaQuantidade)
            
            when (val result = editProdutoInteractor(produtoId, produtoAtualizado)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            produto = produtoAtualizado, 
                            isUpdatingQuantity = false,
                            userMessage = "Estoque atualizado!"
                        ) 
                    }
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Erro ao atualizar estoque."
                    _uiState.update { it.copy(isUpdatingQuantity = false, userMessage = message) }
                }
                else -> {}
            }
        }
    }

    fun removerProduto() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = removeProdutoInteractor(produtoId)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isDeleted = true,
                            userMessage = "Produto removido com sucesso!"
                        ) 
                    }
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Erro ao remover produto."
                    _uiState.update { it.copy(isLoading = false, userMessage = message) }
                }
                else -> {}
            }
        }
    }

    fun snackbarMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
