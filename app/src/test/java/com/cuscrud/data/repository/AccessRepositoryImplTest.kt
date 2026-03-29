package com.cuscrud.data.repository

import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.UserAccessDto
import com.cuscrud.data.remote.dto.UserAccessListResponse
import com.cuscrud.data.remote.dto.InventoryDto
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AccessRepositoryImplTest {

    private lateinit var repository: AccessRepositoryImpl
    private val apiService: CuscrudApiService = mockk()
    private val inventoryRepository: InventoryRepository = mockk()
    private val activeInventoryIdFlow = MutableStateFlow<String?>(null)

    @Before
    fun setup() {
        every { inventoryRepository.activeInventoryId } returns activeInventoryIdFlow
        repository = AccessRepositoryImpl(apiService, inventoryRepository)
    }

    @Test
    fun `getUsers should return Error when no inventory is active`() = runTest {
        activeInventoryIdFlow.value = null

        val result = repository.getUsers()

        assertTrue(result is Result.Error)
        assertEquals("Nenhum inventário ativo selecionado", (result as Result.Error).exception.message)
    }

    @Test
    fun `getUsers should return Success when API responds 200 OK`() = runTest {
        val invId = "inv-123"
        activeInventoryIdFlow.value = invId
        val users = listOf(UserAccessDto("u1", "User 1", "login1", Role.EDITOR.value))
        val inventoryDto = InventoryDto(invId, "Inv Name", Role.OWNER.value)
        val response = UserAccessListResponse(inventoryDto, users)
        
        coEvery { apiService.getInventoryUsers(invId, any(), any()) } returns Response.success(response)

        val result = repository.getUsers()

        assertTrue(result is Result.Success)
        assertEquals(users, (result as Result.Success).data)
    }

    @Test
    fun `addUser should return Success when API responds 200 OK`() = runTest {
        val invId = "inv-123"
        activeInventoryIdFlow.value = invId
        val userDto = UserAccessDto("u1", "User 1", "login1", Role.EDITOR.value)
        
        coEvery { apiService.addInventoryUser(invId, any()) } returns Response.success(userDto)

        val result = repository.addUser("login1", Role.EDITOR)

        assertTrue(result is Result.Success)
        assertEquals(userDto, (result as Result.Success).data)
    }

    @Test
    fun `updateUserRole should return Success when API responds 200 OK`() = runTest {
        val invId = "inv-123"
        activeInventoryIdFlow.value = invId
        val userDto = UserAccessDto("u1", "User 1", "login1", Role.READER.value)
        
        coEvery { apiService.updateInventoryUserRole(invId, "u1", any()) } returns Response.success(userDto)

        val result = repository.updateUserRole("u1", Role.READER)

        assertTrue(result is Result.Success)
        assertEquals(userDto, (result as Result.Success).data)
    }

    @Test
    fun `removeUser should return Success when API responds 204 No Content`() = runTest {
        val invId = "inv-123"
        activeInventoryIdFlow.value = invId
        
        coEvery { apiService.removeInventoryUser(invId, "u1") } returns Response.success(Unit)

        val result = repository.removeUser("u1")

        assertTrue(result is Result.Success)
    }
}
