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
 * Suite de testes unitários para o [InventoryRepositoryImpl] atualizado para DataStore.
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

    @Test
    fun `activeInventoryId and role should be updated when sessionManager flows emit`() = runTest {
        val storedId = "stored-id"
        val storedRole = Role.EDITOR.value
        
        idFlow.value = storedId
        roleFlow.value = storedRole

        assertEquals(storedId, repository.activeInventoryId.value)
        assertEquals(Role.EDITOR, repository.activeInventoryRole.value)
    }

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

    @Test
    fun `getInventories should return Success when API responds 200 OK`() = runTest {
        val inventories = listOf(InventoryDto("1", "Inv 1", 0))
        val response = InventoryListResponse(inventories)
        coEvery { apiService.getInventories(any(), any()) } returns Response.success(response)

        val result = repository.getInventories()

        assertTrue(result is Result.Success)
        assertEquals(inventories, (result as Result.Success).data)
    }

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

    @Test
    fun `getInventories should return Error when API fails`() = runTest {
        coEvery { apiService.getInventories(any(), any()) } returns Response.error(401, "".toResponseBody())

        val result = repository.getInventories()

        assertTrue(result is Result.Error)
    }

    // endregion

    // region Bloco: Remoção e Limpeza de Estado

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
