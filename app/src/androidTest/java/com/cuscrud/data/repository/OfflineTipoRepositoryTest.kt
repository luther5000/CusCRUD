package com.cuscrud.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuscrud.data.local.AppDatabase
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
 * Testes de integração para o [OfflineTipoRepository].
 * 
 * Estes testes validam a persistência e recuperação de categorias (tipos) no banco de dados local,
 * garantindo que a lógica de negócio do repositório e o mapeamento entre banco e domínio
 * estejam funcionando corretamente.
 */
@RunWith(AndroidJUnit4::class)
class OfflineTipoRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: OfflineTipoRepository

    /**
     * Inicializa um banco de dados em memória e o repositório antes de cada teste.
     */
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = OfflineTipoRepository(database.tipoDao())
    }

    /**
     * Fecha o banco de dados após a conclusão dos testes.
     */
    @After
    fun teardown() {
        database.close()
    }

    /**
     * Verifica se o repositório retorna uma lista vazia inicialmente.
     */
    @Test
    fun getAllTipos_whenEmpty_returnsEmptyList() = runBlocking {
        val all = repository.getAllTipos().first()
        assertTrue("A lista de tipos deve estar vazia inicialmente", all.isEmpty())
    }

    /**
     * Testa a inserção de uma nova categoria e sua recuperação.
     */
    @Test
    fun insertAndGetAllTipos_returnsCorrectData() = runBlocking {
        val tipo = TestDataGenerator.createTipo(nome = "Limpeza")
        repository.insertTipo(tipo)

        val all = repository.getAllTipos().first()
        assertEquals(1, all.size)
        assertEquals("Limpeza", all[0].nome)
    }

    /**
     * Valida a remoção de uma categoria por ID.
     */
    @Test
    fun removeTipo_withValidId_removesAndReturnsTipo() = runBlocking {
        val tipo = TestDataGenerator.createTipo(id = 5L, nome = "Perecíveis")
        repository.insertTipo(tipo)

        val removed = repository.removeTipo(5L)

        assertNotNull(removed)
        assertEquals(5L, removed?.id)
        assertEquals("Perecíveis", removed?.nome)
        
        val all = repository.getAllTipos().first()
        assertTrue(all.isEmpty())
    }

    /**
     * Garante que remover um ID inexistente retorne null.
     */
    @Test
    fun removeTipo_withInvalidId_returnsNull() = runBlocking {
        val result = repository.removeTipo(99L)
        assertNull(result)
    }

    /**
     * Testa a edição de uma categoria existente.
     */
    @Test
    fun editTipo_withValidId_updatesData() = runBlocking {
        val initialTipo = TestDataGenerator.createTipo(id = 1L, nome = "Frutas")
        repository.insertTipo(initialTipo)

        val updatedData = TestDataGenerator.createTipo(nome = "Frutas Tropicais")
        val result = repository.editTipo(1L, updatedData)

        assertNotNull(result)
        assertEquals("Frutas Tropicais", result?.nome)
        
        val all = repository.getAllTipos().first()
        assertEquals("Frutas Tropicais", all[0].nome)
    }

    /**
     * Garante que editar uma categoria inexistente retorne null.
     */
    @Test
    fun editTipo_withInvalidId_returnsNull() = runBlocking {
        val updatedData = TestDataGenerator.createTipo(nome = "Inexistente")
        val result = repository.editTipo(123L, updatedData)
        assertNull(result)
    }
}
