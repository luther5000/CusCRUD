package com.cuscrud.presentation.produtos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuscrud.domain.interactor.AddProdutoInteractor
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
    private val getTiposInteractor: GetTiposInteractor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tipoId: Long? = savedStateHandle["tipoId"]

    private val _uiState = MutableStateFlow(AddProdutoUiState())
    val uiState: StateFlow<AddProdutoUiState> = _uiState.asStateFlow()

    init {
        loadTipos()
    }

    private fun loadTipos() {
        viewModelScope.launch {
            getTiposInteractor().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val tipos = result.data
                        _uiState.update { it.copy(tipos = tipos) }
                        
                        // Se houver um tipoId passado pela navegação, seleciona ele
                        if (tipoId != null) {
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

    fun onAddProduto() {
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
            _uiState.update { it.copy(userMessage = "quantidade inválida") }
            return
        }

        val produto = Produto(
            id = 0,
            tipo = tipo,
            marca = currentState.marca,
            dataValidade = currentState.dataValidade,
            unidade = unidadeLong,
            unidadeMedida = currentState.unidadeMedida,
            quantidade = quantidadeLong
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = addProdutoInteractor(produto)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isProductAdded = true,
                            userMessage = "Produto adicionado com sucesso"
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
