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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

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
    
    // Fluxo para simular o ID do inventário ativo
    private val activeInventoryIdFlow = MutableStateFlow<String?>(null)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock padrão para o inventário ativo
        every { inventoryRepository.activeInventoryId } returns activeInventoryIdFlow
        
        repository = RemoteTipoRepository(apiService, inventoryRepository)
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

    /**
     * Objetivo do teste: Garantir que a criação de tipo falhe se não houver um inventário selecionado.
     * Entradas usadas: activeInventoryIdFlow emitindo null.
     * Comportamento esperado: Retorno de Result.Error e a API não deve ser chamada.
     */
    @Test
    fun `insertTipo should return Error and not call API when no active inventory`() = runTest {
        activeInventoryIdFlow.value = null

        val result = repository.insertTipo("Novo Tipo", null)

        assertTrue(result is Result.Error)
        assertEquals("Nenhum inventário ativo selecionado.", (result as Result.Error).exception.message)
        coVerify(exactly = 0) { apiService.createType(any(), any()) }
    }

    // endregion Fim do Bloco: Validação de Contexto

    // region Início do Bloco: Cenários de Sucesso

    /**
     * Objetivo do teste: Validar a listagem de tipos com sucesso.
     * Entradas usadas: activeInventoryId válido e resposta 200 OK da API com lista de tipos.
     * Comportamento esperado: Retorno de Result.Success com a lista mapeada para o domínio.
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
        assertEquals(1L, data[0].id)
    }

    /**
     * Objetivo do teste: Validar a criação de um tipo com sucesso.
     * Entradas usadas: activeInventoryId válido e resposta 201 Created da API.
     * Comportamento esperado: Retorno de Result.Success com o objeto Tipo criado.
     */
    @Test
    fun `insertTipo should return Success when API responds 201 Created`() = runTest {
        val invId = "valid-uuid"
        activeInventoryIdFlow.value = invId
        val tipoDto = TipoDto(10L, "Higiene", hasImage = false)
        
        coEvery { apiService.createType(invId, any()) } returns Response.success(201, tipoDto)

        val result = repository.insertTipo("Higiene", null)

        assertTrue(result is Result.Success)
        assertEquals("Higiene", (result as Result.Success).data.nome)
        assertEquals(10L, result.data.id)
    }

    /**
     * Objetivo do teste: Validar a remoção de um tipo com sucesso.
     * Entradas usadas: activeInventoryId válido e resposta 204 No Content da API.
     * Comportamento esperado: Retorno de Result.Success(Unit).
     */
    @Test
    fun `removeTipo should return Success when API responds 204 No Content`() = runTest {
        val invId = "valid-uuid"
        activeInventoryIdFlow.value = invId
        
        coEvery { apiService.deleteType(invId, 1L) } returns Response.success(204, Unit)

        val result = repository.removeTipo(1L)

        assertTrue(result is Result.Success)
    }

    // endregion Fim do Bloco: Cenários de Sucesso

    // region Início do Bloco: Tratamento de Falhas (Rede e HTTP)

    /**
     * Objetivo do teste: Validar o tratamento de falha de conexão (rede).
     * Entradas usadas: API lançando IOException.
     * Comportamento esperado: Retorno de Result.Error capturando a exceção de IO.
     */
    @Test
    fun `getTipos should return Error when network fails with IOException`() = runTest {
        activeInventoryIdFlow.value = "valid-uuid"
        coEvery { apiService.getTypes(any(), any(), any()) } throws IOException("No internet")

        val result = repository.getTipos()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is IOException)
    }

    /**
     * Objetivo do teste: Validar o tratamento de erro HTTP genérico (ex: 404).
     * Entradas usadas: API retornando Response.error(404).
     * Comportamento esperado: Retorno de Result.Error contendo a HttpException.
     */
    @Test
    fun `getTipoById should return Error when API returns 404 Not Found`() = runTest {
        activeInventoryIdFlow.value = "valid-uuid"
        coEvery { apiService.getTypeById(any(), any()) } returns Response.error(404, "Not Found".toResponseBody())

        val result = repository.getTipoById(1L)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is HttpException)
        assertEquals(404, (result.exception as HttpException).code())
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
        
        // Simula o erro 409 retornado pela API
        coEvery { apiService.deleteType(invId, 1L) } returns Response.error(409, "Conflict".toResponseBody())

        val result = repository.removeTipo(1L)

        assertTrue(result is Result.Error)
        val errorMessage = (result as Result.Error).exception.message
        assertEquals("Não é possível excluir este tipo pois existem produtos vinculados a ele.", errorMessage)
    }

    // endregion Fim do Bloco: Conflict 409
}
