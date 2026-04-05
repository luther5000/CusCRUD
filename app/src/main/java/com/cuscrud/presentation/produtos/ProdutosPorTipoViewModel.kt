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

    private val _uiState = MutableStateFlow(ProdutosPorTipoUiState())
    val uiState: StateFlow<ProdutosPorTipoUiState> = _uiState.asStateFlow()

    companion object {
        const val MAX_VALUE = 999999999999999999L
    }

    init {
        observeUserRole()
        loadProdutos()
    }

    private fun observeUserRole() {
        inventoryRepository.activeInventoryRole
            .onEach { role ->
                _uiState.update { it.copy(userRole = role) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Carrega a lista de produtos do tipo especificado via API.
     */
    fun loadProdutos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
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
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
                Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun alterarQuantidade(produto: Produto, delta: Long) {
        val novaQuantidade = produto.quantidade + delta
        
        if (novaQuantidade < 0) {
            _uiState.update { it.copy(errorMessage = "Não é possível alterar o produto para menos de 0 itens") }
            return
        }
        
        if (novaQuantidade > MAX_VALUE) {
            _uiState.update { it.copy(errorMessage = "Não é possível alterar o produto para mais de $MAX_VALUE itens") }
            return
        }

        val produtoAtualizado = produto.copy(quantidade = novaQuantidade)
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = editProdutoInteractor(produto.id, produtoAtualizado)) {
                is Result.Success -> {
                    // Atualiza a lista localmente para resposta rápida
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            produtos = state.produtos.map { 
                                if (it.id == produto.id) produtoAtualizado else it 
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.exception.message) }
                    loadProdutos() // Recarrega para garantir sincronia em caso de erro
                }
                Result.Loading -> {}
            }
        }
    }

    fun limparMensagens() {
        _uiState.update { it.copy(errorMessage = null, mensagemSucesso = null) }
    }
}
