package com.cuscrud.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuscrud.data.local.AppDatabase
import com.cuscrud.data.local.entities.TipoEntity
import com.cuscrud.testutil.TestDataGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes de integração para o [OfflineProdutoRepository].
 * 
 * Estes testes verificam a interação entre o repositório, os DAOs do Room e os mapeadores,
 * garantindo que a lógica de persistência e recuperação de dados funcione corretamente
 * em um ambiente Android real (ou emulador).
 */
@RunWith(AndroidJUnit4::class)
class OfflineProdutoRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: OfflineProdutoRepository

    /**
     * Configura um banco de dados em memória antes de cada teste para garantir
     * que os testes sejam isolados e não persistam dados entre si.
     */
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = OfflineProdutoRepository(database.produtoDao(), database.tipoDao())
    }

    /**
     * Fecha o banco de dados após a execução de cada teste.
     */
    @After
    fun teardown() {
        database.close()
    }

    /**
     * Verifica se o repositório retorna uma lista vazia quando o banco de dados não possui produtos.
     */
    @Test
    fun getAllProdutos_whenEmpty_returnsEmptyList() = runBlocking {
        val all = repository.getAllProdutos().first()
        assertTrue("Esperava-se uma lista vazia para um banco de dados vazio", all.isEmpty())
    }

    /**
     * Verifica se um produto inserido é recuperado corretamente com todos os seus dados mapeados.
     */
    @Test
    fun insertAndGetAll_returnsCorrectData() = runBlocking {
        val tipo = TestDataGenerator.createTipo(id = 1L)
        database.tipoDao().insert(TipoEntity(tipo.id, tipo.nome, tipo.imagem))

        val produto = TestDataGenerator.createProduto(id = 1, tipo = tipo)
        repository.insertProduto(produto)

        val allProdutos = repository.getAllProdutos().first()
        assertEquals(1, allProdutos.size)
        assertEquals(produto.marca, allProdutos[0].marca)
    }

    /**
     * Valida a lógica de filtragem por tipo de produto, garantindo que apenas
     * os produtos da categoria solicitada sejam retornados.
     */
    @Test
    fun getProdutosByTipo_returnsOnlyMatchingProducts() = runBlocking {
        // Configura duas categorias
        val tipo1 = TestDataGenerator.createTipo(id = 1L, nome = "Eletrônicos")
        val tipo2 = TestDataGenerator.createTipo(id = 2L, nome = "Alimentos")
        database.tipoDao().insert(TipoEntity(tipo1.id, tipo1.nome, tipo1.imagem))
        database.tipoDao().insert(TipoEntity(tipo2.id, tipo2.nome, tipo2.imagem))

        // Insere produtos para ambas
        repository.insertProduto(TestDataGenerator.createProduto(id = 1, tipo = tipo1, marca = "Sony"))
        repository.insertProduto(TestDataGenerator.createProduto(id = 2, tipo = tipo2, marca = "Nestlé"))

        // Quando: Filtrando por Eletrônicos (tipo1)
        val result = repository.getProdutosByTipo(1L).first()

        // Então: Apenas o produto da Sony é retornado
        assertEquals(1, result.size)
        assertEquals("Sony", result[0].marca)
    }

    /**
     * Verifica se a remoção de um produto existente funciona e retorna o objeto removido.
     */
    @Test
    fun removeProduto_withValidId_returnsDeletedProduto() = runBlocking {
        val tipo = TestDataGenerator.createTipo(id = 1L)
        database.tipoDao().insert(TipoEntity(tipo.id, tipo.nome, tipo.imagem))
        repository.insertProduto(TestDataGenerator.createProduto(id = 10, tipo = tipo))

        val removed = repository.removeProduto(10)

        assertNotNull(removed)
        assertEquals(10, removed?.id)
        assertTrue(repository.getAllProdutos().first().isEmpty())
    }

    /**
     * Garante que a tentativa de remover um ID inexistente retorne null sem causar erros.
     */
    @Test
    fun removeProduto_withInvalidId_returnsNull() = runBlocking {
        val removed = repository.removeProduto(999)
        assertNull("Esperava-se null ao remover um produto inexistente", removed)
    }

    /**
     * Testa a edição parcial de um produto, verificando se apenas os campos
     * fornecidos (não vazios) são atualizados.
     */
    @Test
    fun editProduto_withValidId_updatesFields() = runBlocking {
        val tipo = TestDataGenerator.createTipo(id = 1L)
        database.tipoDao().insert(TipoEntity(tipo.id, tipo.nome, tipo.imagem))
        repository.insertProduto(TestDataGenerator.createProduto(id = 1, marca = "Original", quantidade = 5, tipo = tipo))

        val updateInfo = TestDataGenerator.createProduto(marca = "Updated", quantidade = 0)
        val updated = repository.editProduto(1, updateInfo)

        assertNotNull(updated)
        assertEquals("Updated", updated?.marca)
        assertEquals(5L, updated?.quantidade) // 0 foi ignorado conforme a implementação
    }

    /**
     * Garante que a tentativa de editar um produto inexistente retorne null.
     */
    @Test
    fun editProduto_withInvalidId_returnsNull() = runBlocking {
        val updateInfo = TestDataGenerator.createProduto(marca = "Fantasma")
        val result = repository.editProduto(404, updateInfo)
        assertNull("Esperava-se null ao editar um produto inexistente", result)
    }
}
