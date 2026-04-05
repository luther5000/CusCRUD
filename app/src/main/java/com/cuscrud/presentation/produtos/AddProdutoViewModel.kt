package com.cuscrud.presentation.produtos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.*
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddProdutoViewModel @Inject constructor(
    private val addProdutoInteractor: AddProdutoInteractor,
    private val editProdutoInteractor: EditProdutoInteractor,
    private val getProdutoDetalhesInteractor: GetProdutoDetalhesInteractor,
    private val addTipoInteractor: AddTipoInteractor,
    private val getTiposInteractor: GetTiposInteractor,
    private val inventoryRepository: InventoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tipoId: Long? = savedStateHandle.get<Long>("tipoId")
    private val produtoId: Long? = savedStateHandle.get<Long>("produtoId")

    private val _uiState = MutableStateFlow(AddProdutoUiState())
    val uiState: StateFlow<AddProdutoUiState> = _uiState.asStateFlow()

    companion object {
        const val MAX_NAME_LENGTH = 255
        const val MAX_VALUE = 999999999999999999L
    }

    init {
        observeUserRole()
        loadTipos()

        if (produtoId != null && produtoId > 0) {
            _uiState.update { it.copy(isEditMode = true) }
            loadProduto(produtoId)
        }
    }

    private fun observeUserRole() {
        inventoryRepository.activeInventoryRole
            .onEach { role ->
                _uiState.update { it.copy(userRole = role) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadTipos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getTiposInteractor()) {
                is Result.Success -> {
                    val tipos = result.data
                    _uiState.update { it.copy(tipos = tipos, isLoading = false) }
                    
                    if (tipoId != null && tipoId > 0L && !uiState.value.isEditMode) {
                        val tipoPreSelecionado = tipos.find { it.id == tipoId }
                        if (tipoPreSelecionado != null) {
                            _uiState.update { it.copy(tipoSelecionado = tipoPreSelecionado) }
                        }
                    }
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Erro ao carregar categorias."
                    _uiState.update { it.copy(userMessage = message, isLoading = false) }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun loadProduto(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getProdutoDetalhesInteractor(id)) {
                is Result.Success -> {
                    val produto = result.data
                    _uiState.update {
                        it.copy(
                            marca = produto.marca,
                            unidade = produto.unidade.toString(),
                            unidadeMedida = produto.unidadeMedida,
                            quantidade = produto.quantidade.toString(),
                            dataValidade = produto.dataValidade,
                            tipoSelecionado = produto.tipo,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Erro ao carregar detalhes do produto."
                    _uiState.update { it.copy(userMessage = message, isLoading = false) }
                }
                Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun onMarcaChanged(marca: String) {
        _uiState.update { it.copy(marca = marca) }
    }

    fun onUnidadeChanged(unidade: String) {
        _uiState.update { it.copy(unidade = unidade) }
    }

    fun onUnidadeMedidaChanged(unidadeMedida: String) {
        _uiState.update { it.copy(unidadeMedida = unidadeMedida) }
    }

    fun onQuantidadeChanged(quantidade: String) {
        _uiState.update { it.copy(quantidade = quantidade) }
    }

    fun onDataValidadeChanged(dataValidade: Date) {
        _uiState.update { it.copy(dataValidade = dataValidade) }
    }

    fun onTipoSelected(tipo: Tipo) {
        _uiState.update { it.copy(tipoSelecionado = tipo) }
    }

    fun onAddNovoTipo(nome: String) {
        if (nome.isBlank()) {
            _uiState.update { it.copy(userMessage = "O nome da categoria é obrigatório.") }
            return
        }
        if (nome.length > MAX_NAME_LENGTH) {
            _uiState.update { it.copy(userMessage = "O nome da categoria é muito longo.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = addTipoInteractor(nome)) {
                is Result.Success -> {
                    val novoTipo = result.data
                    _uiState.update { 
                        it.copy(
                            tipos = it.tipos + novoTipo,
                            tipoSelecionado = novoTipo,
                            isLoading = false,
                            userMessage = "Categoria '${nome}' criada com sucesso!"
                        ) 
                    }
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Erro ao criar nova categoria."
                    _uiState.update { it.copy(isLoading = false, userMessage = message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun onSaveProduto() {
        val currentState = _uiState.value
        
        val tipo = currentState.tipoSelecionado
        if (tipo == null) {
            _uiState.update { it.copy(userMessage = "Selecionar um tipo de produto é obrigatório.") }
            return
        }

        if (currentState.marca.isBlank()) {
            _uiState.update { it.copy(userMessage = "O preenchimento da marca é obrigatório.") }
            return
        }
        if (currentState.marca.length > MAX_NAME_LENGTH) {
            _uiState.update { it.copy(userMessage = "O nome da marca é muito longo.") }
            return
        }

        if (currentState.unidade.isBlank()) {
            _uiState.update { it.copy(userMessage = "O preenchimento da unidade é obrigatório.") }
            return
        }
        val unidadeLong = currentState.unidade.toLongOrNull()
        if (unidadeLong == null || unidadeLong > MAX_VALUE) {
            _uiState.update { it.copy(userMessage = "Valor unitário muito grande.") }
            return
        }
        if (unidadeLong < 0) {
            _uiState.update { it.copy(userMessage = "Valor unitário inválido.") }
            return
        }

        if (currentState.quantidade.isBlank()) {
            _uiState.update { it.copy(userMessage = "O preenchimento da quantidade é obrigatório.") }
            return
        }
        val quantidadeLong = currentState.quantidade.toLongOrNull()
        if (quantidadeLong == null || quantidadeLong > MAX_VALUE) {
            _uiState.update { it.copy(userMessage = "Quantidade muito grande.") }
            return
        }
        if (quantidadeLong < 0) {
            _uiState.update { it.copy(userMessage = "Quantidade inválida.") }
            return
        }

        val produto = Produto(
            id = if (currentState.isEditMode) (produtoId ?: 0) else 0,
            tipo = tipo,
            marca = currentState.marca,
            dataValidade = currentState.dataValidade,
            unidade = unidadeLong,
            unidadeMedida = currentState.unidadeMedida,
            quantidade = quantidadeLong
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (currentState.isEditMode) {
                editProdutoInteractor(produto.id, produto)
            } else {
                addProdutoInteractor(produto)
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isProductSaved = true,
                            userMessage = if (currentState.isEditMode) "Produto editado com sucesso" else "Produto adicionado com sucesso"
                        ) 
                    }
                }
                is Result.Error -> {
                    val message = result.exception.message ?: "Erro ao salvar o produto."
                    _uiState.update { it.copy(isLoading = false, userMessage = message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun snackbarMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
