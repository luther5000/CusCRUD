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
 * Suite de testes unitários atualizada para o [RemoteProdutoRepository].
 * 
 * Valida a dependência do repositório em relação ao inventário ativo e o
 * tratamento de erro quando nenhum contexto de inventário está selecionado.
 */
class RemoteProdutoRepositoryTest {

    private lateinit var repository: RemoteProdutoRepository
    private val apiService: CuscrudApiService = mockk()
    private val inventoryRepository: InventoryRepository = mockk()
    
    private val activeInventoryFlow = MutableStateFlow<String?>(null)

    @Before
    fun setup() {
        every { inventoryRepository.activeInventoryId } returns activeInventoryFlow
        // O construtor agora exige apiService e inventoryRepository
        repository = RemoteProdutoRepository(apiService, inventoryRepository)
    }

    // region Bloco: Validação de Contexto (Inventário Ativo)

    /**
     * Objetivo: Impedir chamadas de API quando não há inventário selecionado.
     * Entradas: inventoryRepository.activeInventoryId emitindo null.
     * Comportamento esperado: Retornar lista vazia e NÃO chamar a API.
     */
    @Test
    fun `getAllProdutos should not call API and return empty when no inventory is active`() = runTest {
        activeInventoryFlow.value = null

        val result = repository.getAllProdutos().first()

        assertTrue(result.isEmpty())
        // getProducts é suspend, deve usar coVerify
        coVerify(exactly = 0) { apiService.getProducts(any(), any(), any()) }
    }

    /**
     * Objetivo: Permitir chamadas de API quando há um inventário selecionado.
     * Entradas: activeInventoryId = "valid-uuid".
     * Comportamento esperado: Chamar a API usando o UUID correto no path.
     */
    @Test
    fun `getAllProdutos should call API with active inventory ID`() = runTest {
        val invId = "valid-uuid"
        activeInventoryFlow.value = invId
        coEvery { apiService.getProducts(invId, any(), any()) } returns retrofit2.Response.success(emptyList())

        repository.getAllProdutos().first()

        // getProducts é suspend, deve usar coVerify
        coVerify { apiService.getProducts(invId, any(), any()) }
    }

    // endregion

    // region Bloco: Operações de Escrita sem Contexto

    /**
     * Objetivo: Validar que inserção falha sem inventário ativo.
     * Entradas: activeInventoryId = null.
     * Comportamento esperado: API de addProduct não deve ser chamada.
     */
    @Test
    fun `insertProduto should not call API when no inventory is active`() = runTest {
        activeInventoryFlow.value = null
        val produto = mockk<com.cuscrud.domain.model.Produto>(relaxed = true)

        repository.insertProduto(produto)

        // addProduct é suspend, deve usar coVerify
        coVerify(exactly = 0) { apiService.addProduct(any(), any()) }
    }

    // endregion
}
