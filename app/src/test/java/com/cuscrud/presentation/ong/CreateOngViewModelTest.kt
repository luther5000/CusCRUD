package com.cuscrud.presentation.ong

import com.cuscrud.domain.interactor.CreateOngInteractor
import com.cuscrud.domain.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Testes unitários para o [CreateOngViewModel].
 * Baseado nos cenários Gherkin de criação de uma nova ONG.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateOngViewModelTest {

    private val createOngInteractor: CreateOngInteractor = mockk()
    private lateinit var viewModel: CreateOngViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CreateOngViewModel(createOngInteractor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Cenário: Criação de uma nova ONG com sucesso
     */
    @Test
    fun `quando criar com nome valido, deve sinalizar sucesso`() = runTest {
        // Dado
        val name = "Minha Nova ONG"
        coEvery { createOngInteractor(name) } returns Result.Success(mockk())

        // Quando
        viewModel.onNameChanged(name)
        viewModel.onCreateClick()

        // Então
        assertTrue(viewModel.uiState.value.isOngCreated)
        assertNull(viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    /**
     * Cenário: Tentativa de criação com nome em branco
     */
    @Test
    fun `quando nome estiver em branco, deve exibir erro de validacao`() = runTest {
        // Quando
        viewModel.onNameChanged("")
        viewModel.onCreateClick()

        // Então
        assertEquals("O preenchimento do nome é obrigatório.", viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.isOngCreated)
    }

    /**
     * Cenário: Falha de conexão ao criar uma nova ONG
     */
    @Test
    fun `quando ocorrer erro de conexao, deve exibir mensagem amigavel`() = runTest {
        // Dado
        val name = "ONG Teste"
        // Simulando erro de conexão (IOException)
        coEvery { createOngInteractor(name) } returns Result.Error(IOException())

        // Quando
        viewModel.onNameChanged(name)
        viewModel.onCreateClick()

        // Então
        assertEquals("Não foi possível comunicar com o servidor. Tente novamente mais tarde.", viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.isOngCreated)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
