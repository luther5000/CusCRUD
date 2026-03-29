package com.cuscrud.data.repository

import com.cuscrud.data.local.SessionManager
import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.data.remote.dto.InventoryListResponse
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Suite de testes unitários para o [InventoryRepositoryImpl].
 *
 * Valida a gestão de estado reativo (StateFlow), a persistência local via SessionManager
 * e a integração com a API de inventários, seguindo o padrão Result.
 */
class InventoryRepositoryImplTest {

    private lateinit var repository: InventoryRepositoryImpl
    private val apiService: CuscrudApiService = mockk()
    private val sessionManager: SessionManager = mockk(relaxed = true)

    @Before
    fun setup() {
        // Simula ausência de inventário salvo por padrão
        every { sessionManager.fetchActiveInventoryId() } returns null
        every { sessionManager.fetchActiveInventoryRole() } returns -1
        repository = InventoryRepositoryImpl(apiService, sessionManager)
    }

    // region Bloco: Inicialização e Estado (StateFlow)

    /**
     * Objetivo: Validar se o repositório inicializa o StateFlow com o valor do SessionManager.
     */
    @Test
    fun `activeInventoryId and role should be initialized with values from sessionManager`() = runTest {
        val storedId = "stored-id"
        val storedRole = Role.EDITOR.value
        every { sessionManager.fetchActiveInventoryId() } returns storedId
        every { sessionManager.fetchActiveInventoryRole() } returns storedRole
        
        // Reinicializa para ler o mock
        val repo = InventoryRepositoryImpl(apiService, sessionManager)

        assertEquals(storedId, repo.activeInventoryId.value)
        assertEquals(Role.EDITOR, repo.activeInventoryRole.value)
    }

    /**
     * Objetivo: Validar a alternância de inventário ativo.
     */
    @Test
    fun `setActiveInventory should update sessionManager and stateFlow`() = runTest {
        val newId = "new-id"
        val role = Role.OWNER

        repository.setActiveInventory(newId, role)

        verify { sessionManager.saveActiveInventoryId(newId) }
        verify { sessionManager.saveActiveInventoryRole(role.value) }
        assertEquals(newId, repository.activeInventoryId.value)
        assertEquals(role, repository.activeInventoryRole.value)
    }

    // endregion

    // region Bloco: CRUD Inventários

    /**
     * Objetivo: Validar listagem de inventários com sucesso.
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
     * Objetivo: Validar atualização automática da role ao listar inventários se o ativo estiver na lista.
     */
    @Test
    fun `getInventories should update active role if current active inventory is in the list`() = runTest {
        repository.setActiveInventory("id-1", Role.READER)
        
        val inventories = listOf(InventoryDto("id-1", "Inv 1", Role.EDITOR.value))
        val response = InventoryListResponse(inventories)
        coEvery { apiService.getInventories(any(), any()) } returns Response.success(response)

        repository.getInventories()

        assertEquals(Role.EDITOR, repository.activeInventoryRole.value)
        verify { sessionManager.saveActiveInventoryRole(Role.EDITOR.value) }
    }

    /**
     * Objetivo: Validar tratamento de erro na API de listagem.
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
     * Objetivo: Validar exclusão de inventário que NÃO é o ativo.
     */
    @Test
    fun `deleteInventory should not clear state if deleted ID is not the active one`() = runTest {
        repository.setActiveInventory("id-A", Role.OWNER)
        coEvery { apiService.deleteInventory("id-B") } returns Response.success(Unit)

        repository.deleteInventory("id-B")

        assertEquals("id-A", repository.activeInventoryId.value)
        verify(exactly = 0) { sessionManager.clearActiveInventoryId() }
    }

    /**
     * Objetivo: Validar exclusão do inventário que É o ativo.
     */
    @Test
    fun `deleteInventory should clear state if deleted ID is the active one`() = runTest {
        repository.setActiveInventory("id-A", Role.OWNER)
        coEvery { apiService.deleteInventory("id-A") } returns Response.success(Unit)

        repository.deleteInventory("id-A")

        assertNull(repository.activeInventoryId.value)
        assertNull(repository.activeInventoryRole.value)
        verify { sessionManager.clearActiveInventoryId() }
        verify { sessionManager.clearActiveInventoryRole() }
    }

    // endregion
}
