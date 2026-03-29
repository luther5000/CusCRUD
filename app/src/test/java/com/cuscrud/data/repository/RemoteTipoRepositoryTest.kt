package com.cuscrud.data.repository

import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.TipoDto
import com.cuscrud.data.remote.dto.TipoListResponse
import com.cuscrud.domain.repository.InventoryRepository
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
 * Suite de testes unitários para o [RemoteTipoRepository].
 * 
 * Esta classe valida a gestão de categorias (Tipos) via API, assegurando o isolamento
 * por inventário e o tratamento adequado de erros HTTP (404, 409) e falhas de rede.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteTipoRepositoryTest {

    private lateinit var repository: RemoteTipoRepository
    private val apiService: CuscrudApiService = mockk()
    private val inventoryRepository: InventoryRepository = mockk()
    private val json = Json { ignoreUnknownKeys = true }
    
    private val activeInventoryIdFlow = MutableStateFlow<String?>(null)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { inventoryRepository.activeInventoryId } returns activeInventoryIdFlow
        repository = RemoteTipoRepository(apiService, inventoryRepository, json)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // region Bloco: Validação de Contexto

    /**
     * Objetivo: Impedir operações sem um inventário ativo selecionado.
     * Entradas: activeInventoryId nulo.
     * Critério de Aceitação: Retornar Result.Error e não disparar chamadas de rede.
     */
    @Test
    fun `getTipos should return Error and not call API when no active inventory`() = runTest {
        activeInventoryIdFlow.value = null

        val result = repository.getTipos()

        assertTrue(result is Result.Error)
        assertEquals("Nenhum inventário ativo selecionado.", (result as Result.Error).exception.message)
        coVerify(exactly = 0) { apiService.getTypes(any(), any(), any()) }
    }

    // endregion

    // region Bloco: Listagem de Tipos

    /**
     * Objetivo: Validar a recuperação bem-sucedida das categorias do inventário.
     * Entradas: API retornando 200 OK com uma lista de tipos.
     * Critério de Aceitação: Retornar Result.Success com os dados mapeados corretamente.
     */
    @Test
    fun `getTipos should return Success when API responds 200 OK`() = runTest {
        val invId = "valid-uuid"
        activeInventoryIdFlow.value = invId
        val typesDto = listOf(TipoDto(1L, "Alimentos", hasImage = false))
        val responseBody = TipoListResponse(types = typesDto, nextPage = null)
        
        coEvery { apiService.getTypes(invId, any(), any()) } returns Response.success(responseBody)

        val result = repository.getTipos()

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals("Alimentos", data[0].nome)
    }

    // endregion

    // region Bloco: Tratamento de Erros e Exceções

    /**
     * Objetivo: Tratar falhas físicas de comunicação (timeout, sem internet).
     * Entradas: API lançando IOException.
     * Critério de Aceitação: Retornar Result.Error with mensagem de falha de conexão.
     */
    @Test
    fun `getTipos should return Error with friendly message when network fails`() = runTest {
        activeInventoryIdFlow.value = "valid-uuid"
        coEvery { apiService.getTypes(any(), any(), any()) } throws java.io.IOException("No internet")

        val result = repository.getTipos()

        assertTrue(result is Result.Error)
        assertEquals("Falha de conexão ao buscar categorias. Verifique sua internet.", (result as Result.Error).exception.message)
    }

    /**
     * Objetivo: Tratar recursos inexistentes.
     * Entradas: API retornando 404 Not Found para um ID específico.
     * Critério de Aceitação: Retornar Result.Error com mensagem informando que a categoria não existe.
     */
    @Test
    fun `getTipoById should return Error with friendly message when API returns 404 Not Found`() = runTest {
        activeInventoryIdFlow.value = "valid-uuid"
        coEvery { apiService.getTypeById(any(), 1L) } returns Response.error(404, "".toResponseBody())

        val result = repository.getTipoById(1L)

        assertTrue(result is Result.Error)
        assertEquals("A categoria não foi encontrada.", (result as Result.Error).exception.message)
    }

    /**
     * Objetivo: Validar integridade referencial ao excluir um tipo.
     * Entradas: API retornando 409 Conflict (provavelmente devido a produtos vinculados).
     * Critério de Aceitação: Retornar Result.Error com instrução clara ao usuário sobre o impedimento.
     */
    @Test
    fun `removeTipo should return friendly Error message when API returns 409 Conflict`() = runTest {
        val invId = "valid-uuid"
        activeInventoryIdFlow.value = invId
        
        coEvery { apiService.deleteType(invId, 1L) } returns Response.error(409, "".toResponseBody())

        val result = repository.removeTipo(1L)

        assertTrue(result is Result.Error)
        val errorMessage = (result as Result.Error).exception.message
        assertEquals("Não é possível excluir este tipo pois existem produtos vinculados a ele.", errorMessage)
    }

    // endregion
}
