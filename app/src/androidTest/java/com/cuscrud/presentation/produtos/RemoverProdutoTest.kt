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
        solicitarRemocaoDoProduto()
        verificarProdutoNoEstadoParaRemocao()
    }

    @Test
    fun cancelarRemocao_deveLimparProdutoParaRemoverNoState() {
        solicitarRemocaoDoProduto()
        cancelarRemocaoDoProduto()
        verificarProdutoParaRemocaoLimpado()
    }

    @Test
    fun confirmarRemocao_comSucesso_deveChamarInteractorEAtualizarEstado() = runTest {
        solicitarRemocaoDoProduto()
        simularSucessoNaRemocao()
        confirmarRemocao()

        verificarInteracaoComRepositorioParaRemover()
        verificarProdutoParaRemocaoLimpado()
        verificarMensagemDeSucesso()
    }

    @Test
    fun confirmarRemocao_comErro_deveFecharODialogoEMostrarErro() = runTest {
        solicitarRemocaoDoProduto()
        simularErroNaRemocao()
        confirmarRemocao()

        verificarInteracaoComRepositorioParaRemover()
        verificarProdutoParaRemocaoLimpado()
        verificarMensagemDeErro()
    }

    // =========================================================
    // Métodos de Ação (DSL de Teste)
    // =========================================================

    private fun solicitarRemocaoDoProduto() {
        viewModel.solicitarRemocao(sampleProduto)
    }

    private fun cancelarRemocaoDoProduto() {
        viewModel.cancelarRemocao()
    }

    private fun confirmarRemocao() {
        viewModel.confirmarRemocao()
    }

    private fun simularSucessoNaRemocao() {
        coEvery { removeProdutoInteractor.invoke(sampleProduto) } returns sampleProduto
    }

    private fun simularErroNaRemocao() {
        coEvery { removeProdutoInteractor.invoke(sampleProduto) } returns null
    }

    // =========================================================
    // Métodos de Verificação
    // =========================================================

    private fun verificarProdutoNoEstadoParaRemocao() {
        assertEquals(sampleProduto, viewModel.uiState.value.produtoParaRemover)
    }

    private fun verificarProdutoParaRemocaoLimpado() {
        assertNull(viewModel.uiState.value.produtoParaRemover)
    }

    private fun verificarInteracaoComRepositorioParaRemover() {
        coVerify(exactly = 1) { removeProdutoInteractor.invoke(sampleProduto) }
    }

    private fun verificarMensagemDeSucesso() {
        assertEquals("${sampleProduto.marca} removido com sucesso", viewModel.uiState.value.mensagemSucesso)
    }

    private fun verificarMensagemDeErro() {
        assertEquals(
            "Não foi possível realizar a remoção: Produto não encontrado ou erro no banco.",
            viewModel.uiState.value.errorMessage
        )
    }
}
