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
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
            MaterialTheme {
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

    init {
        setupInitialData()
    }

    // Função dummy apenas para garantir a instanciação do VM
    fun touch() {}

    private fun setupInitialData() {
        viewModelScope.launch {
            val result = tipoRepository.getTipos()
            val tiposAtuais = if (result is Result.Success) result.data else emptyList()
            
            if (tiposAtuais.isEmpty()) {
                // Inserção síncrona dentro da coroutine para garantir ordem
                tipoRepository.insertTipo(nome = "Carnes")
                tipoRepository.insertTipo(nome = "Laticínios")
                tipoRepository.insertTipo(nome = "Bebidas")

                // Busca as categorias inseridas para pegar o ID gerado
                val updatedResult = tipoRepository.getTipos()
                if (updatedResult is Result.Success) {
                    val primeiraCategoria = updatedResult.data.firstOrNull()
                    
                    primeiraCategoria?.let { tipo ->
                        val produtoTeste = Produto(
                            id = 0,
                            tipo = tipo,
                            marca = "Produto de Teste Inicial",
                            dataValidade = Date(),
                            unidade = 1,
                            unidadeMedida = "kg",
                            quantidade = 10
                        )
                        produtoRepository.insertProduto(produtoTeste)
                    }
                }
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
            }
        }
    }

    val produtos: StateFlow<List<Produto>> = produtoRepository.getAllProdutos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
