package com.cuscrud.presentation.produtos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.EditProdutoInteractor
import com.cuscrud.domain.interactor.GetProdutosPorTipoInteractor
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProdutosPorTipoViewModel @Inject constructor(
    private val getProdutosPorTipoInteractor: GetProdutosPorTipoInteractor,
    private val editProdutoInteractor: EditProdutoInteractor,
    private val inventoryRepository: InventoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tipoId: Long = checkNotNull(savedStateHandle["tipoId"])

    private val _uiState = MutableStateFlow(ProdutosPorTipoUiState(isLoading = true))
    val uiState: StateFlow<ProdutosPorTipoUiState> = _uiState.asStateFlow()

    companion object {
        const val MAX_VALUE = 999999999999999999L
    }

    private var isRefreshing = false

    init {
        observeUserRole()
    }

    private fun observeUserRole() {
        inventoryRepository.activeInventoryRole
            .onEach { role ->
                _uiState.update { it.copy(userRole = role) }
            }
            .launchIn(viewModelScope)
    }

    fun loadProdutos() {
        if (isRefreshing) return

        viewModelScope.launch {
            val currentState = _uiState.value
            // Só mostra o loading de tela cheia se a lista estiver vazia
            val shouldShowFullLoading = currentState.produtos.isEmpty()

            if (shouldShowFullLoading) {
                _uiState.update { it.copy(isLoading = true) }
            } else {
                isRefreshing = true
            }
            
            when (val result = getProdutosPorTipoInteractor(tipoId)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(isLoading = false, produtos = result.data, errorMessage = null) 
                    }
                }
                is Result.Error -> {
                    val message = if (result.exception is java.io.IOException) {
                        "Falha de conexão. Verifique sua internet."
                    } else {
                        result.exception.message ?: "Erro ao carregar produtos."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = if (shouldShowFullLoading) message else null) }
                }
                Result.Loading -> {
                    if (shouldShowFullLoading) _uiState.update { it.copy(isLoading = true) }
                }
            }
            isRefreshing = false
        }
    }

    fun alterarQuantidade(produto: Produto, delta: Long) {
        val produtoAtual = _uiState.value.produtos.find { it.id == produto.id } ?: produto
        val novaQuantidade = produtoAtual.quantidade + delta
        
        if (novaQuantidade < 0) {
            _uiState.update { it.copy(errorMessage = "Não é possível alterar o produto para menos de 0 itens") }
            return
        }
        
        if (novaQuantidade > MAX_VALUE) {
            _uiState.update { it.copy(errorMessage = "Não é possível alterar o produto para mais de $MAX_VALUE itens") }
            return
        }

        val produtoAtualizado = produtoAtual.copy(quantidade = novaQuantidade)
        
        _uiState.update { state ->
            state.copy(
                produtos = state.produtos.map { 
                    if (it.id == produto.id) produtoAtualizado else it 
                }
            )
        }

        viewModelScope.launch {
            when (val result = editProdutoInteractor(produto.id, produtoAtualizado)) {
                is Result.Success -> {}
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message) }
                    loadProdutos()
                }
                Result.Loading -> {}
            }
        }
    }

    fun limparMensagens() {
        _uiState.update { it.copy(errorMessage = null, mensagemSucesso = null) }
    }
}
