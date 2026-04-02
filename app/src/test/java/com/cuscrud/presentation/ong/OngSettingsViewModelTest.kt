package com.cuscrud.presentation.ong

import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.domain.interactor.UpdateOngInteractor
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import io.mockk.coEvery
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

        viewModel = OngSettingsViewModel(repository, updateOngInteractor)
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
}
