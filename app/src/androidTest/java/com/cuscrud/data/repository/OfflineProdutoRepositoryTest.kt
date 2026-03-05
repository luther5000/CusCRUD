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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineProdutoRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: OfflineProdutoRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = OfflineProdutoRepository(database.produtoDao(), database.tipoDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetAll_returnsCorrectData() = runBlocking {
        // Given: A category exists in the DB
        val tipo = TestDataGenerator.createTipo(id = 1L)
        database.tipoDao().insert(TipoEntity(tipo.id, tipo.nome, tipo.imagem))

        // When: A product is inserted via repository
        val produto = TestDataGenerator.createProduto(id = 1, tipo = tipo)
        repository.insertProduto(produto)

        // Then: It is retrieved correctly
        val allProdutos = repository.getAllProdutos().first()
        assertEquals(1, allProdutos.size)
        assertEquals(produto.marca, allProdutos[0].marca)
        assertEquals(produto.tipo.nome, allProdutos[0].tipo.nome)
    }

    @Test
    fun removeProduto_returnsDeletedProduto() = runBlocking {
        // Given: A product exists
        val tipo = TestDataGenerator.createTipo(id = 1L)
        database.tipoDao().insert(TipoEntity(tipo.id, tipo.nome, tipo.imagem))
        val produto = TestDataGenerator.createProduto(id = 10, tipo = tipo)
        repository.insertProduto(produto)

        // When: Removing by ID
        val removed = repository.removeProduto(10)

        // Then: It returns the removed item and database is empty
        assertNotNull(removed)
        assertEquals(10, removed?.id)
        val all = repository.getAllProdutos().first()
        assertEquals(0, all.size)
    }

    @Test
    fun editProduto_updatesOnlySpecifiedFields() = runBlocking {
        // Given: A product exists
        val tipo = TestDataGenerator.createTipo(id = 1L)
        database.tipoDao().insert(TipoEntity(tipo.id, tipo.nome, tipo.imagem))
        val initialProduto = TestDataGenerator.createProduto(id = 1, marca = "Original", quantidade = 5, tipo = tipo)
        repository.insertProduto(initialProduto)

        // When: Editing only the brand
        val updateInfo = TestDataGenerator.createProduto(marca = "Updated", quantidade = 0) // quantity 0 means ignore in our impl
        val updated = repository.editProduto(1, updateInfo)

        // Then: Brand is updated, quantity remains the same
        assertNotNull(updated)
        assertEquals("Updated", updated?.marca)
        assertEquals(5L, updated?.quantidade)
    }
}
