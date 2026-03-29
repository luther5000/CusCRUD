package com.cuscrud.data.repository

import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
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
 * Suite de testes unitários para o [RemoteProdutoRepository].
 *
 * Esta classe valida a integração com o serviço remoto de produtos, assegurando que todas as
 * operações (leitura, escrita e remoção) dependam de um inventário ativo.
 * Também verifica o tratamento de erros HTTP e a correta conversão das respostas da API
 * para o padrão [Result] utilizado pela camada de domínio.
 */
class RemoteProdutoRepositoryTest {

    private lateinit var repository: RemoteProdutoRepository
    private val apiService: CuscrudApiService = mockk()
    private val inventoryRepository: InventoryRepository = mockk()
    private val json = Json { ignoreUnknownKeys = true }
    
    private val activeInventoryFlow = MutableStateFlow<String?>(null)

    @Before
    fun setup() {
        every { inventoryRepository.activeInventoryId } returns activeInventoryFlow
        repository = RemoteProdutoRepository(apiService, inventoryRepository, json)
    }

    // region Bloco: Validação de Contexto (Inventário Ativo)

    /**
     * Objetivo: Impedir a busca de produtos sem um inventário ativo selecionado.
     * Entradas: Estado de inventário ativo como nulo.
     * Critério de Aceitação: Retornar [Result.Error] com mensagem apropriada e não invocar a API.
     */
    @Test
    fun `getProdutos should return Error when no inventory is active`() = runTest {
        activeInventoryFlow.value = null

        val result = repository.getProdutos()

        assertTrue(result is Result.Error)
        assertEquals("Nenhum inventário ativo selecionado.", (result as Result.Error).exception.message)
        coVerify(exactly = 0) { apiService.getProducts(any(), any(), any()) }
    }

    /**
     * Objetivo: Validar a recuperação de produtos quando há um contexto válido.
     * Entradas: ID de inventário ativo e resposta 200 OK da API.
     * Critério de Aceitação: Retornar [Result.Success] e invocar o endpoint correto.
     */
    @Test
    fun `getProdutos should call API and return Success when inventory is active`() = runTest {
        val invId = "valid-uuid"
        activeInventoryFlow.value = invId
        coEvery { apiService.getProducts(invId, any(), any()) } returns Response.success(emptyList())

        val result = repository.getProdutos()

        assertTrue(result is Result.Success)
        coVerify { apiService.getProducts(invId, any(), any()) }
    }

    // endregion

    // region Bloco: Tratamento de Erros da API

    /**
     * Objetivo: Propagar mensagens de erro específicas enviadas pelo servidor.
     * Entradas: Resposta 404 com corpo JSON contendo código e mensagem.
     * Critério de Aceitação: O [Result.Error] deve conter a mensagem exata retornada pela API.
     */
    @Test
    fun `getProdutos should return Error with message from API on failure`() = runTest {
        val invId = "valid-uuid"
        activeInventoryFlow.value = invId
        val errorJson = """{"error": {"code": "NOT_FOUND", "message": "Inventário não encontrado"}}"""
        coEvery { apiService.getProducts(invId, any(), any()) } returns Response.error(404, errorJson.toResponseBody())

        val result = repository.getProdutos()

        assertTrue(result is Result.Error)
        assertEquals("Inventário não encontrado", (result as Result.Error).exception.message)
    }

    /**
     * Objetivo: Tratar erros genéricos do servidor quando não há corpo de erro detalhado.
     * Entradas: Resposta 500 com corpo vazio.
     * Critério de Aceitação: Retornar uma mensagem de fallback amigável ao usuário.
     */
    @Test
    fun `getProdutos should return fallback Error message when API returns invalid error body`() = runTest {
        val invId = "valid-uuid"
        activeInventoryFlow.value = invId
        coEvery { apiService.getProducts(invId, any(), any()) } returns Response.error(500, "".toResponseBody())

        val result = repository.getProdutos()

        assertTrue(result is Result.Error)
        assertEquals("Erro interno no servidor. Tente novamente em instantes.", (result as Result.Error).exception.message)
    }

    // endregion

    // region Bloco: Operações de Escrita

    /**
     * Objetivo: Impedir a inserção de produtos sem contexto de inventário.
     * Entradas: activeInventoryId nulo.
     * Critério de Aceitação: Retornar [Result.Error] e não realizar a chamada de rede.
     */
    @Test
    fun `insertProduto should return Error when no inventory is active`() = runTest {
        activeInventoryFlow.value = null
        val produto = mockk<com.cuscrud.domain.model.Produto>(relaxed = true)

        val result = repository.insertProduto(produto)

        assertTrue(result is Result.Error)
        assertEquals("Identificador de inventário não encontrado.", (result as Result.Error).exception.message)
        coVerify(exactly = 0) { apiService.addProduct(any(), any()) }
    }

    /**
     * Objetivo: Confirmar a criação bem-sucedida de um produto.
     * Entradas: Dados do produto e inventário ativo.
     * Critério de Aceitação: Retornar [Result.Success] após confirmação da API.
     */
    @Test
    fun `insertProduto should return Success when API call is successful`() = runTest {
        val invId = "valid-uuid"
        activeInventoryFlow.value = invId
        val produto = mockk<com.cuscrud.domain.model.Produto>(relaxed = true)
        coEvery { apiService.addProduct(invId, any()) } returns Response.success(mockk(relaxed = true))

        val result = repository.insertProduto(produto)

        assertTrue(result is Result.Success)
        coVerify { apiService.addProduct(invId, any()) }
    }

    /**
     * Objetivo: Validar a remoção de um produto.
     * Entradas: ID do produto e inventário ativo.
     * Critério de Aceitação: Invocar o endpoint de deleção e retornar [Result.Success].
     */
    @Test
    fun `removeProduto should call API and return Success`() = runTest {
        val invId = "valid-uuid"
        activeInventoryFlow.value = invId
        coEvery { apiService.deleteProduct(invId, 1) } returns Response.success(Unit)

        val result = repository.removeProduto(1)

        assertTrue(result is Result.Success)
        coVerify { apiService.deleteProduct(invId, 1) }
    }

    // endregion
}
