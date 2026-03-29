package com.cuscrud.data.repository

import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.domain.repository.InventoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de testes unitários para o [RemoteProdutoRepository].
 * 
 * Esta classe valida a integração com a API de produtos, focando na obrigatoriedade
 * de um contexto de inventário ativo para todas as operações. Garante que o repositório
 * não realize chamadas indevidas quando nenhum inventário está selecionado.
 */
class RemoteProdutoRepositoryTest {

    private lateinit var repository: RemoteProdutoRepository
    private val apiService: CuscrudApiService = mockk()
    private val inventoryRepository: InventoryRepository = mockk()
    
    private val activeInventoryFlow = MutableStateFlow<String?>(null)

    @Before
    fun setup() {
        every { inventoryRepository.activeInventoryId } returns activeInventoryFlow
        repository = RemoteProdutoRepository(apiService, inventoryRepository)
    }

    // region Bloco: Validação de Contexto (Inventário Ativo)

    /**
     * Objetivo: Impedir a busca de produtos sem um inventário selecionado.
     * Entradas: activeInventoryId emitindo null.
     * Critério de Aceitação: Retornar uma lista vazia imediatamente e NÃO invocar a API.
     */
    @Test
    fun `getAllProdutos should not call API and return empty when no inventory is active`() = runTest {
        activeInventoryFlow.value = null

        val result = repository.getAllProdutos().first()

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { apiService.getProducts(any(), any(), any()) }
    }

    /**
     * Objetivo: Garantir que a busca de produtos utilize o inventário correto.
     * Entradas: activeInventoryId com um UUID válido.
     * Critério de Aceitação: Chamar o endpoint da API passando o ID do inventário ativo no path.
     */
    @Test
    fun `getAllProdutos should call API with active inventory ID`() = runTest {
        val invId = "valid-uuid"
        activeInventoryFlow.value = invId
        coEvery { apiService.getProducts(invId, any(), any()) } returns retrofit2.Response.success(emptyList())

        repository.getAllProdutos().first()

        coVerify { apiService.getProducts(invId, any(), any()) }
    }

    // endregion

    // region Bloco: Operações de Escrita sem Contexto

    /**
     * Objetivo: Impedir a criação de produtos sem contexto.
     * Entradas: activeInventoryId nulo.
     * Critério de Aceitação: Abortar a operação silenciosamente sem realizar chamadas de rede.
     */
    @Test
    fun `insertProduto should not call API when no inventory is active`() = runTest {
        activeInventoryFlow.value = null
        val produto = mockk<com.cuscrud.domain.model.Produto>(relaxed = true)

        repository.insertProduto(produto)

        coVerify(exactly = 0) { apiService.addProduct(any(), any()) }
    }

    // endregion
}
