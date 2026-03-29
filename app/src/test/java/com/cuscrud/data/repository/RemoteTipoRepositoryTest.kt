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
 * Esta classe valida a integração com a API REST, o tratamento de erros HTTP e a dependência do contexto de inventário.
 *
 * Segue rigorosamente a Seção 7.2 do architecture.md para documentação e TDD.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteTipoRepositoryTest {

    private lateinit var repository: RemoteTipoRepository
    private val apiService: CuscrudApiService = mockk()
    private val inventoryRepository: InventoryRepository = mockk()
    private val json = Json { ignoreUnknownKeys = true }
    
    // Fluxo para simular o ID do inventário ativo
    private val activeInventoryIdFlow = MutableStateFlow<String?>(null)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock padrão para o inventário ativo
        every { inventoryRepository.activeInventoryId } returns activeInventoryIdFlow
        
        repository = RemoteTipoRepository(apiService, inventoryRepository, json)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // region Início do Bloco: Validação de Contexto (Inventário Ativo)

    /**
     * Objetivo do teste: Garantir que operações falhem se não houver um inventário selecionado.
     * Entradas usadas: activeInventoryIdFlow emitindo null.
     * Comportamento esperado: Retorno de Result.Error e a API não deve ser chamada.
     */
    @Test
    fun `getTipos should return Error and not call API when no active inventory`() = runTest {
        activeInventoryIdFlow.value = null

        val result = repository.getTipos()

        assertTrue(result is Result.Error)
        assertEquals("Nenhum inventário ativo selecionado.", (result as Result.Error).exception.message)
        coVerify(exactly = 0) { apiService.getTypes(any(), any(), any()) }
    }

    // endregion Fim do Bloco: Validação de Contexto

    // region Início do Bloco: Cenários de Sucesso

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

    // endregion Fim do Bloco: Cenários de Sucesso

    // region Início do Bloco: Tratamento de Falhas (Rede e HTTP)

    /**
     * Objetivo do teste: Validar o tratamento de falha de conexão (rede).
     * Entradas usadas: API lançando IOException.
     * Comportamento esperado: Retorno de Result.Error capturando a exceção de IO com mensagem amigável.
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
     * Objetivo do teste: Validar o tratamento de erro HTTP genérico (ex: 404) usando o handleError.
     * Entradas usadas: API retornando Response.error(404).
     * Comportamento esperado: Retorno de Result.Error contendo a mensagem mapeada no repositório.
     */
    @Test
    fun `getTipoById should return Error with friendly message when API returns 404 Not Found`() = runTest {
        activeInventoryIdFlow.value = "valid-uuid"
        coEvery { apiService.getTypeById(any(), any()) } returns Response.error(404, "".toResponseBody())

        val result = repository.getTipoById(1L)

        assertTrue(result is Result.Error)
        assertEquals("A categoria não foi encontrada.", (result as Result.Error).exception.message)
    }

    // endregion Fim do Bloco: Tratamento de Falhas

    // region Início do Bloco: Regra de Negócio Específica (Conflict 409)

    /**
     * Objetivo do teste: Validar o tratamento de erro 409 Conflict na remoção (ON DELETE RESTRICT).
     * Entradas usadas: API retornando status 409.
     * Comportamento esperado: Retorno de Result.Error com mensagem amigável sobre produtos vinculados.
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

    // endregion Fim do Bloco: Conflict 409
}
