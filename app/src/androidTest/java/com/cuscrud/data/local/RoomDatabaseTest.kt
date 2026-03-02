package com.cuscrud.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuscrud.data.local.dao.ProdutoDao
import com.cuscrud.data.local.dao.TipoDao
import com.cuscrud.data.local.entities.ProdutoEntity
import com.cuscrud.data.local.entities.TipoEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {
    private lateinit var produtoDao: ProdutoDao
    private lateinit var tipoDao: TipoDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        produtoDao = db.produtoDao()
        tipoDao = db.tipoDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeProdutoAndReadInList() = runBlocking {
        val produto = ProdutoEntity(
            id = 1,
            tipo = "Eletrônico",
            marca = "Samsung",
            dataValidade = System.currentTimeMillis(),
            unidade = 1,
            unidadeMedida = "un",
            quantidade = 10
        )
        produtoDao.insert(produto)
        val allProdutos = produtoDao.getAll()
        assertEquals(allProdutos[0].marca, "Samsung")
    }

    @Test
    @Throws(Exception::class)
    fun writeTipoAndReadInList() = runBlocking {
        // First insert a product because of ForeignKey constraint in TipoEntity
        val produto = ProdutoEntity(
            id = 1,
            tipo = "Eletrônico",
            marca = "Samsung",
            dataValidade = System.currentTimeMillis(),
            unidade = 1,
            unidadeMedida = "un",
            quantidade = 10
        )
        produtoDao.insert(produto)

        val tipo = TipoEntity(
            id = 1, // Must match the product ID due to ForeignKey in your current definition
            nome = "Smartphones",
            imagem = byteArrayOf(0x01)
        )
        tipoDao.insert(tipo)
        val allTipos = tipoDao.getAll()
        assertEquals(allTipos[0].nome, "Smartphones")
    }
}