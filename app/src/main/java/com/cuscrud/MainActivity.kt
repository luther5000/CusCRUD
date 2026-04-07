package com.cuscrud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.repository.TipoRepository
import com.cuscrud.domain.util.Result
import com.cuscrud.presentation.navigation.CusCrudNavGraph
import com.cuscrud.ui.theme.CusCRUDTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Instancia o ViewModel
    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Garante que o init do ViewModel foi chamado
        viewModel.touch() 

        setContent {
            CusCRUDTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    CusCrudNavGraph(
                        navController = navController,
                        mainActivity = this
                    )
                }
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val produtoRepository: ProdutoRepository,
    private val tipoRepository: TipoRepository
) : ViewModel() {

    private val _produtos = MutableStateFlow<List<Produto>>(emptyList())
    val produtos: StateFlow<List<Produto>> = _produtos.asStateFlow()

    init {
        // Removido o setupInitialData() que inseria produtos de teste
        fetchProdutos()
    }

    // Função dummy apenas para garantir a instanciação do VM
    fun touch() {}

    /**
     * Busca produtos da API para manter o estado local do MainViewModel.
     */
    fun fetchProdutos() {
        viewModelScope.launch {
            when (val result = produtoRepository.getProdutos()) {
                is Result.Success -> _produtos.value = result.data
                else -> { /* Log ou erro silencioso para o MainViewModel */ }
            }
        }
    }

    fun addSampleProduct() {
        viewModelScope.launch {
            val result = tipoRepository.getTipos()
            val types = if (result is Result.Success) result.data else emptyList()
            
            val tipo = if (types.isNotEmpty()) {
                types.first()
            } else {
                val insertResult = tipoRepository.insertTipo(nome = "Geral")
                if (insertResult is Result.Success) insertResult.data else null
            }

            tipo?.let { selectedTipo ->
                val newProduto = Produto(
                    id = 0,
                    tipo = selectedTipo,
                    marca = "Exemplo ${System.currentTimeMillis() % 1000}",
                    dataValidade = Date(),
                    unidade = 1,
                    unidadeMedida = "un",
                    quantidade = (1..20).random().toLong()
                )
                produtoRepository.insertProduto(newProduto)
                fetchProdutos() // Atualiza a lista após adicionar
            }
        }
    }
}
