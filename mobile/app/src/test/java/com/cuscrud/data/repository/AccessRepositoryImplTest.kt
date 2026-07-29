package com.cuscrud.data.repository

import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.*
import com.cuscrud.domain.model.Role
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
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
 * respeitem o contexto do inventário ativo e tratem corretamente as respostas da API,
 * incluindo o parse de mensagens de erro detalhadas.
 */
class AccessRepositoryImplTest {

    private lateinit var repository: AccessRepositoryImpl
    private val apiService: CuscrudApiService = mockk()
    private val inventoryRepository: InventoryRepository = mockk()
    private val json = Json { ignoreUnknownKeys = true }
    private val activeInventoryIdFlow = MutableStateFlow<String?>(null)

    @Before
    fun setup() {
        every { inventoryRepository.activeInventoryId } returns activeInventoryIdFlow
        repository = AccessRepositoryImpl(apiService, inventoryRepository, json)
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
     * Objetivo: Validar a extração de mensagem de erro customizada da API.
     * Entradas: Resposta HTTP 403 com JSON de erro padronizado.
     * Critério de Aceitação: O Result.Error deve conter a mensagem "Usuário sem permissão" extraída do JSON.
     */
    @Test
    fun `addUser should return Error with message from API JSON on failure`() = runTest {
        val invId = "inv-123"
        activeInventoryIdFlow.value = invId
        val errorJson = """{"error": {"code": "FORBIDDEN", "message": "Usuário sem permissão"}}"""
        
        coEvery { apiService.addInventoryUser(invId, any()) } returns Response.error(403, errorJson.toResponseBody())

        val result = repository.addUser("login1", Role.EDITOR)

        assertTrue(result is Result.Error)
        assertEquals("Usuário sem permissão", (result as Result.Error).exception.message)
    }

    /**
     * Objetivo: Garantir fallback para mensagens genéricas quando o JSON de erro é inválido.
     * Entradas: Resposta HTTP 401 com corpo vazio.
     * Critério de Aceitação: Retornar Result.Error com a mensagem amigável de fallback para 401.
     */
    @Test
    fun `updateUserRole should return fallback message when API error JSON is invalid`() = runTest {
        val invId = "inv-123"
        activeInventoryIdFlow.value = invId
        
        coEvery { apiService.updateInventoryUserRole(invId, "u1", any()) } returns Response.error(401, "".toResponseBody())

        val result = repository.updateUserRole("u1", Role.READER)

        assertTrue(result is Result.Error)
        assertEquals("Sessão expirada. Por favor, faça login novamente.", (result as Result.Error).exception.message)
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
