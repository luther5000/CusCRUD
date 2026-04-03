package com.cuscrud.presentation.ong

import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.domain.interactor.DeleteOngInteractor
import com.cuscrud.domain.interactor.UpdateOngInteractor
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class OngSettingsViewModelTest {

    private val repository: InventoryRepository = mockk()
    private val updateOngInteractor: UpdateOngInteractor = mockk()
    private val deleteOngInteractor: DeleteOngInteractor = mockk()
    private lateinit var viewModel: OngSettingsViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    private val activeInventoryIdFlow = MutableStateFlow<String?>(null)
    private val activeInventoryRoleFlow = MutableStateFlow<Role?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        coEvery { repository.activeInventoryId } returns activeInventoryIdFlow
        coEvery { repository.activeInventoryRole } returns activeInventoryRoleFlow
        coEvery { repository.getInventories() } returns Result.Success(emptyList())

        viewModel = OngSettingsViewModel(repository, updateOngInteractor, deleteOngInteractor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `quando o dono edita o nome com sucesso, deve atualizar o estado`() = runTest {
        // Dado
        val ongId = "123"
        val oldName = "Nome Antigo"
        val newName = "Nome Novo"
        val ongs = listOf(InventoryDto(ongId, oldName, Role.OWNER.value))

        activeInventoryIdFlow.value = ongId
        activeInventoryRoleFlow.value = Role.OWNER
        coEvery { repository.getInventories() } returns Result.Success(ongs)
        coEvery { updateOngInteractor(ongId, newName) } returns Result.Success(InventoryDto(ongId, newName, Role.OWNER.value))

        // Quando
        viewModel.onToggleEdit()
        viewModel.onEditNameChanged(newName)
        viewModel.onSaveClick()

        // Então
        assertEquals(newName, viewModel.uiState.value.ongName)
        assertFalse(viewModel.uiState.value.isEditing)
        assertEquals("Nome da ONG atualizado com sucesso!", viewModel.uiState.value.userMessage)
    }

    @Test
    fun `quando o dono tenta salvar nome em branco, deve exibir erro de validacao`() = runTest {
        // Dado
        val ongId = "123"
        activeInventoryIdFlow.value = ongId
        activeInventoryRoleFlow.value = Role.OWNER
        coEvery { updateOngInteractor(ongId, "") } returns Result.Error(IllegalArgumentException("O preenchimento do nome é obrigatório."))

        // Quando
        viewModel.onToggleEdit()
        viewModel.onEditNameChanged("")
        viewModel.onSaveClick()

        // Então
        assertEquals("O preenchimento do nome é obrigatório.", viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.isEditing)
    }

    @Test
    fun `quando um editor tenta editar, deve exibir mensagem de bloqueio`() = runTest {
        // Dado
        activeInventoryRoleFlow.value = Role.EDITOR

        // Quando
        viewModel.onToggleEdit()

        // Então
        assertEquals("Apenas o dono pode realizar esta ação.", viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.isEditing)
    }

    @Test
    fun `quando ocorre falha de conexao ao salvar, deve exibir mensagem amigavel`() = runTest {
        // Dado
        val ongId = "123"
        val newName = "Nome Novo"
        activeInventoryIdFlow.value = ongId
        activeInventoryRoleFlow.value = Role.OWNER
        coEvery { updateOngInteractor(ongId, newName) } returns Result.Error(IOException())

        // Quando
        viewModel.onToggleEdit()
        viewModel.onEditNameChanged(newName)
        viewModel.onSaveClick()

        // Então
        assertEquals("Não foi possível comunicar com o servidor. Tente novamente mais tarde.", viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    // --- Testes de Remoção de ONG ---

    @Test
    fun `quando o dono confirma a remocao com sucesso, deve atualizar para sucesso`() = runTest {
        // Dado (Cenário: Remover a ONG com sucesso)
        val ongId = "123"
        activeInventoryIdFlow.value = ongId
        activeInventoryRoleFlow.value = Role.OWNER
        coEvery { deleteOngInteractor(ongId) } returns Result.Success(Unit)

        // Quando
        viewModel.onDeleteClick() // Abre o alerta
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        
        viewModel.onConfirmDelete() // Confirma no alerta

        // Então
        assertEquals("ONG removida com sucesso!", viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        coVerify { deleteOngInteractor(ongId) }
    }

    @Test
    fun `quando o dono cancela a remocao, nao deve chamar o interactor nem remover nada`() = runTest {
        // Dado (Cenário: Cancelar a remoção da ONG)
        val ongId = "123"
        activeInventoryIdFlow.value = ongId
        activeInventoryRoleFlow.value = Role.OWNER

        // Quando
        viewModel.onDeleteClick()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        
        viewModel.onCancelDelete()

        // Então
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertFalse(viewModel.uiState.value.isSuccess)
        coVerify(exactly = 0) { deleteOngInteractor(any()) }
    }

    @Test
    fun `quando um editor tenta remover, deve exibir mensagem de bloqueio e nao abrir o alerta`() = runTest {
        // Dado (Cenário: Bloqueio de remoção para utilizadores sem permissão)
        activeInventoryRoleFlow.value = Role.EDITOR

        // Quando
        viewModel.onDeleteClick()

        // Então
        assertEquals("Apenas o dono pode realizar esta ação.", viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun `quando ocorre falha de conexao ao remover, deve exibir mensagem amigavel e manter a ong`() = runTest {
        // Dado (Cenário: Falha de ligação ao remover a ONG)
        val ongId = "123"
        activeInventoryIdFlow.value = ongId
        activeInventoryRoleFlow.value = Role.OWNER
        coEvery { deleteOngInteractor(ongId) } returns Result.Error(IOException())

        // Quando
        viewModel.onDeleteClick()
        viewModel.onConfirmDelete()

        // Então
        assertEquals("Não foi possível comunicar com o servidor. Tente novamente mais tarde.", viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
