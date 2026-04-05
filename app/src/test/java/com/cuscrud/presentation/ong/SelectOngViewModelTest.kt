package com.cuscrud.presentation.ong

import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.domain.interactor.GetOngsInteractor
import com.cuscrud.domain.interactor.SetActiveOngInteractor
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários para o [SelectOngViewModel].
 * Baseado nos cenários Gherkin de alternância entre ONGs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectOngViewModelTest {

    private val getOngsInteractor: GetOngsInteractor = mockk()
    private val setActiveOngInteractor: SetActiveOngInteractor = mockk(relaxed = true)
    private lateinit var viewModel: SelectOngViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Cenário: Alternar entre inventários com sucesso
     * Verifica se ao carregar as ONGs com sucesso, o estado é atualizado corretamente.
     */
    @Test
    fun `loadOngs com sucesso deve atualizar a lista de ongs no uiState`() = runTest {
        // Dado
        val ongs = listOf(
            InventoryDto("1", "ONG A", 0), // OWNER
            InventoryDto("2", "ONG B", 1)  // EDITOR
        )
        coEvery { getOngsInteractor() } returns Result.Success(ongs)

        // Quando
        viewModel = SelectOngViewModel(getOngsInteractor, setActiveOngInteractor)

        // Então
        assertEquals(ongs, viewModel.uiState.value.ongs)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    /**
     * Cenário: Alternar entre inventários com sucesso
     * Verifica se ao selecionar uma ONG, o contexto ativo é atualizado.
     */
    @Test
    fun `onOngSelected com sucesso deve definir ong como ativa e sinalizar sucesso`() = runTest {
        // Dado
        val ong = InventoryDto("1", "ONG A", 0)
        coEvery { getOngsInteractor() } returns Result.Success(listOf(ong))
        viewModel = SelectOngViewModel(getOngsInteractor, setActiveOngInteractor)

        // Quando
        viewModel.onOngSelected(ong)

        // Então
        coVerify { setActiveOngInteractor("1", Role.OWNER) }
        assertTrue(viewModel.uiState.value.isOngSelected)
    }

    /**
     * Cenário: Falha de conexão ao alternar entre ONGs
     * Verifica se erro ao carregar lista de ONGs exibe mensagem amigável.
     */
    @Test
    fun `loadOngs com erro deve exibir mensagem de erro no uiState`() = runTest {
        // Dado
        val errorMessage = "Falha de conexão ao buscar inventários."
        coEvery { getOngsInteractor() } returns Result.Error(Exception(errorMessage))

        // Quando
        viewModel = SelectOngViewModel(getOngsInteractor, setActiveOngInteractor)

        // Então
        assertEquals(errorMessage, viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    /**
     * Cenário: Falha de conexão ao alternar entre ONGs
     * Verifica se erro ao selecionar ONG exibe mensagem e mantém estado de falha.
     */
    @Test
    fun `onOngSelected com erro deve exibir mensagem de falha no carregamento do inventario`() = runTest {
        // Dado
        val ong = InventoryDto("1", "ONG A", 0)
        coEvery { getOngsInteractor() } returns Result.Success(listOf(ong))
        coEvery { setActiveOngInteractor(any(), any()) } throws Exception("Erro interno")
        
        viewModel = SelectOngViewModel(getOngsInteractor, setActiveOngInteractor)

        // Quando
        viewModel.onOngSelected(ong)

        // Então
        assertEquals("Não foi possível carregar o inventário da ONG selecionada.", viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.isOngSelected)
    }
}
