package com.cuscrud.data.repository

import com.cuscrud.data.local.SessionManager
import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.data.remote.dto.InventoryListResponse
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.util.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Suite de testes unitários para o [InventoryRepositoryImpl].
 * 
 * Esta classe valida a gestão de inventários (CRUD) e a sincronização do estado global
 * do inventário ativo (ID e Role) entre o repositório e o SessionManager (DataStore).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InventoryRepositoryImplTest {

    private lateinit var repository: InventoryRepositoryImpl
    private val apiService: CuscrudApiService = mockk()
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }

    // Fluxos para simular o SessionManager
    private val idFlow = MutableStateFlow<String?>(null)
    private val roleFlow = MutableStateFlow(-1)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { sessionManager.activeInventoryIdFlow } returns idFlow
        every { sessionManager.activeInventoryRoleFlow } returns roleFlow
        
        repository = InventoryRepositoryImpl(apiService, sessionManager, json)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Bloco: Inicialização e Estado (StateFlow)

    /**
     * Objetivo: Garantir que o repositório reflita mudanças no DataStore em tempo real.
     * Entradas: Emissões de novos valores nos fluxos do SessionManager.
     * Critério de Aceitação: Os StateFlows do repositório devem expor o ID e Role corretos.
     */
    @Test
    fun `activeInventoryId and role should be updated when sessionManager flows emit`() = runTest {
        val storedId = "stored-id"
        val storedRole = Role.EDITOR.value
        
        idFlow.value = storedId
        roleFlow.value = storedRole

        assertEquals(storedId, repository.activeInventoryId.value)
        assertEquals(Role.EDITOR, repository.activeInventoryRole.value)
    }

    /**
     * Objetivo: Validar a persistência da seleção de inventário.
     * Entradas: Chamada para setActiveInventory com novo ID e Role.
     * Critério de Aceitação: Invocar os métodos de salvamento correspondentes no SessionManager.
     */
    @Test
    fun `setActiveInventory should call sessionManager suspend methods`() = runTest {
        val newId = "new-id"
        val role = Role.OWNER

        repository.setActiveInventory(newId, role)

        coVerify { sessionManager.saveActiveInventoryId(newId) }
        coVerify { sessionManager.saveActiveInventoryRole(role.value) }
    }

    // endregion

    // region Bloco: CRUD Inventários

    /**
     * Objetivo: Validar a listagem de inventários do usuário.
     * Entradas: API retornando lista de InventoryDto.
     * Critério de Aceitação: Retornar Result.Success com a lista mapeada.
     */
    @Test
    fun `getInventories should return Success when API responds 200 OK`() = runTest {
        val inventories = listOf(InventoryDto("1", "Inv 1", 0))
        val response = InventoryListResponse(inventories)
        coEvery { apiService.getInventories(any(), any()) } returns Response.success(response)

        val result = repository.getInventories()

        assertTrue(result is Result.Success)
        assertEquals(inventories, (result as Result.Success).data)
    }

    /**
     * Objetivo: Sincronizar permissões locais com o servidor ao listar inventários.
     * Entradas: Inventário ativo atual está na lista retornada pela API com uma Role diferente.
     * Critério de Aceitação: Atualizar a Role no SessionManager para refletir o estado atual do servidor.
     */
    @Test
    fun `getInventories should update active role via sessionManager if current active inventory is in the list`() = runTest {
        // Simula inventário ativo "id-1" com role READER (2)
        idFlow.value = "id-1"
        roleFlow.value = Role.READER.value
        
        // API retorna que agora a role no servidor é EDITOR (1)
        val inventories = listOf(InventoryDto("id-1", "Inv 1", Role.EDITOR.value))
        val response = InventoryListResponse(inventories)
        coEvery { apiService.getInventories(any(), any()) } returns Response.success(response)

        repository.getInventories()

        coVerify { sessionManager.saveActiveInventoryRole(Role.EDITOR.value) }
    }

    /**
     * Objetivo: Tratar falhas na listagem de inventários.
     * Entradas: Resposta de erro da API (401).
     * Critério de Aceitação: Retornar Result.Error.
     */
    @Test
    fun `getInventories should return Error when API fails`() = runTest {
        coEvery { apiService.getInventories(any(), any()) } returns Response.error(401, "".toResponseBody())

        val result = repository.getInventories()

        assertTrue(result is Result.Error)
    }

    // endregion

    // region Bloco: Remoção e Limpeza de Estado

    /**
     * Objetivo: Limpar o contexto ativo se o inventário selecionado for excluído.
     * Entradas: Exclusão bem-sucedida de um inventário que é o "activeInventoryId".
     * Critério de Aceitação: Invocar métodos de limpeza no SessionManager para evitar referências a IDs inexistentes.
     */
    @Test
    fun `deleteInventory should clear session via sessionManager if deleted ID is the active one`() = runTest {
        idFlow.value = "id-A"
        coEvery { apiService.deleteInventory("id-A") } returns Response.success(Unit)

        repository.deleteInventory("id-A")

        coVerify { sessionManager.clearActiveInventoryId() }
        coVerify { sessionManager.clearActiveInventoryRole() }
    }

    // endregion
}
