package com.cuscrud.presentation.produtos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.AddProdutoInteractor
import com.cuscrud.domain.interactor.EditProdutoInteractor
import com.cuscrud.domain.interactor.GetProdutoDetalhesInteractor
import com.cuscrud.domain.interactor.GetTiposInteractor
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddProdutoViewModel @Inject constructor(
    private val addProdutoInteractor: AddProdutoInteractor,
    private val editProdutoInteractor: EditProdutoInteractor,
    private val getProdutoDetalhesInteractor: GetProdutoDetalhesInteractor,
    private val getTiposInteractor: GetTiposInteractor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tipoId: Long? = savedStateHandle.get<Long>("tipoId")
    private val produtoId: Int? = savedStateHandle.get<Int>("produtoId")

    private val _uiState = MutableStateFlow(AddProdutoUiState())
    val uiState: StateFlow<AddProdutoUiState> = _uiState.asStateFlow()

    init {
        loadTipos()

        // No Room, IDs auto-gerados costumam começar em 1. 
        // Se produtoId for > 0, entramos em modo de edição.
        if (produtoId != null && produtoId > 0) {
            _uiState.update { it.copy(isEditMode = true) }
            loadProduto(produtoId)
        }
    }

    private fun loadTipos() {
        viewModelScope.launch {
            getTiposInteractor().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val tipos = result.data
                        _uiState.update { it.copy(tipos = tipos) }
                        
                        // Se houver um tipoId passado pela navegação, seleciona ele
                        // apenas se não estivermos editando um produto existente
                        if (tipoId != null && tipoId > 0L && !uiState.value.isEditMode) {
                            val tipoPreSelecionado = tipos.find { it.id == tipoId }
                            if (tipoPreSelecionado != null) {
                                _uiState.update { it.copy(tipoSelecionado = tipoPreSelecionado) }
                            }
                        }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(userMessage = result.exception.message) }
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    private fun loadProduto(id: Int) {
        viewModelScope.launch {
            getProdutoDetalhesInteractor(id).collect { result ->
                when (result) {
                    is Result.Success -> {
                        result.data?.let { produto ->
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
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(userMessage = result.exception.message, isLoading = false) }
                    }
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
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

    fun onSaveProduto() {
        val currentState = _uiState.value
        
        val tipo = currentState.tipoSelecionado
        if (tipo == null) {
            _uiState.update { it.copy(userMessage = "Selecione um tipo de produto") }
            return
        }

        val unidadeLong = currentState.unidade.toLongOrNull()
        if (unidadeLong == null || unidadeLong < 0) {
            _uiState.update { it.copy(userMessage = "unidade inválida") }
            return
        }

        val quantidadeLong = currentState.quantidade.toLongOrNull()
        if (quantidadeLong == null || quantidadeLong < 0) {
            val msg = "é necessário informar uma quantidade positiva para fazer a adição"
            _uiState.update { it.copy(userMessage = msg) }
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
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            userMessage = result.exception.message 
                        ) 
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun snackbarMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
