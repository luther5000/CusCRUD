package com.cuscrud.presentation.produtos

import androidx.lifecycle.SavedStateHandle
import com.cuscrud.domain.interactor.GetProdutosPorTipoInteractor
import com.cuscrud.domain.interactor.RemoveProdutoInteractor
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class RemoverProdutoTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private val getProdutosPorTipoInteractor: GetProdutosPorTipoInteractor = mockk()
    private val removeProdutoInteractor: RemoveProdutoInteractor = mockk()
    private lateinit var viewModel: ProdutosPorTipoViewModel

    private val sampleTipo = Tipo(1L, "Proteína", byteArrayOf())
    private val sampleProduto = Produto(
        id = 1,
        tipo = sampleTipo,
        marca = "Frango",
        dataValidade = Date(),
        unidade = 1,
        unidadeMedida = "kg",
        quantidade = 2
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mocking the flow returned by the first interactor which is called in init
        every { getProdutosPorTipoInteractor.invoke(any()) } returns flowOf(Result.Success(emptyList()))
        
        viewModel = ProdutosPorTipoViewModel(
            getProdutosPorTipoInteractor = getProdutosPorTipoInteractor,
            removeProdutoInteractor = removeProdutoInteractor,
            savedStateHandle = SavedStateHandle(mapOf("tipoId" to 1L))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun solicitarRemocao_deveAtualizarProdutoParaRemoverNoState() {
        // Ação
        viewModel.solicitarRemocao(sampleProduto)

        // Validação
        assertEquals(sampleProduto, viewModel.uiState.value.produtoParaRemover)
    }

    @Test
    fun cancelarRemocao_deveLimparProdutoParaRemoverNoState() {
        // Setup
        viewModel.solicitarRemocao(sampleProduto)
        
        // Ação
        viewModel.cancelarRemocao()

        // Validação
        assertNull(viewModel.uiState.value.produtoParaRemover)
    }

    @Test
    fun confirmarRemocao_comSucesso_deveChamarInteractorEAtualizarEstado() = runTest {
        // Setup
        viewModel.solicitarRemocao(sampleProduto)
        coEvery { removeProdutoInteractor.invoke(sampleProduto) } returns Result.Success(Unit)

        // Ação
        viewModel.confirmarRemocao()

        // Validações
        coVerify(exactly = 1) { removeProdutoInteractor.invoke(sampleProduto) }
        assertNull(viewModel.uiState.value.produtoParaRemover)
        assertEquals("Produto removido com sucesso", viewModel.uiState.value.mensagemSucesso)
    }

    @Test
    fun confirmarRemocao_comErro_deveFecharODialogoEMostrarErro() = runTest {
        // Setup
        viewModel.solicitarRemocao(sampleProduto)
        coEvery { removeProdutoInteractor.invoke(sampleProduto) } returns Result.Error(Exception("Falha na rede"))

        // Ação
        viewModel.confirmarRemocao()

        // Validações
        coVerify(exactly = 1) { removeProdutoInteractor.invoke(sampleProduto) }
        assertNull(viewModel.uiState.value.produtoParaRemover)
        assertEquals("Não foi possível realizar a remoção: Falha na rede", viewModel.uiState.value.errorMessage)
    }
}