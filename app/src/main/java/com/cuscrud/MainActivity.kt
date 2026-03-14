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
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.repository.TipoRepository
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

    // ViewModel de teste mantida para facilitar a geração de dados iniciais
    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    val produtos: StateFlow<List<Produto>> = produtoRepository.getAllProdutos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Função auxiliar para testar a persistência e visualização.
     */
    fun addSampleProduct() {
        viewModelScope.launch {
            // Garante a existência de um tipo para evitar erros de integridade
            val sampleTipo = Tipo(id = 1L, nome = "Alimentos de Teste", imagem = byteArrayOf(0x01))
            tipoRepository.insertTipo(sampleTipo)

            val newProduto = Produto(
                id = 0,
                tipo = sampleTipo,
                marca = "Marca de Teste ${System.currentTimeMillis() % 1000}",
                dataValidade = Date(),
                unidade = 10,
                unidadeMedida = "kg",
                quantidade = (1..50).random().toLong()
            )
            produtoRepository.insertProduto(newProduto)
        }
    }
}