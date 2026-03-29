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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Suite de testes unitários para o [AccessRepositoryImpl].
 * 
 * Esta classe valida a gestão de permissões de usuários em inventários compartilhados.
 * Garante que as operações de listagem, adição, atualização e remoção de membros
 * respeitem o contexto do inventário ativo e tratem corretamente as respostas da API.
 */
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

    /**
     * Objetivo: Impedir a listagem de usuários sem um inventário selecionado.
     * Entradas: Fluxo de inventário ativo emitindo null.
     * Critério de Aceitação: Retornar Result.Error com mensagem de erro de contexto.
     */
    @Test
    fun `getUsers should return Error when no inventory is active`() = runTest {
        activeInventoryIdFlow.value = null

        val result = repository.getUsers()

        assertTrue(result is Result.Error)
        assertEquals("Nenhum inventário ativo selecionado", (result as Result.Error).exception.message)
    }

    /**
     * Objetivo: Validar a recuperação da lista de usuários com acesso ao inventário.
     * Entradas: ID de inventário ativo válido e resposta de sucesso da API.
     * Critério de Aceitação: Retornar Result.Success contendo a lista de UserAccessDto.
     */
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

    /**
     * Objetivo: Validar a adição de um novo usuário ao inventário.
     * Entradas: Login do usuário, Role desejada e API retornando sucesso.
     * Critério de Aceitação: Retornar Result.Success com os dados do novo acesso criado.
     */
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

    /**
     * Objetivo: Validar a alteração do nível de permissão de um usuário existente.
     * Entradas: UserID, nova Role e resposta de sucesso da API.
     * Critério de Aceitação: Retornar Result.Success com o DTO atualizado.
     */
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

    /**
     * Objetivo: Validar a revogação de acesso de um usuário ao inventário.
     * Entradas: UserID e resposta 204 No Content da API.
     * Critério de Aceitação: Retornar Result.Success vazio indicando sucesso na remoção.
     */
    @Test
    fun `removeUser should return Success when API responds 204 No Content`() = runTest {
        val invId = "inv-123"
        activeInventoryIdFlow.value = invId
        
        coEvery { apiService.removeInventoryUser(invId, "u1") } returns Response.success(Unit)

        val result = repository.removeUser("u1")

        assertTrue(result is Result.Success)
    }
}
