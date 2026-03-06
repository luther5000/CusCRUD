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

/**
 * Testes de integração de baixo nível para o banco de dados Room.
 * 
 * Esta classe testa diretamente os DAOs ([ProdutoDao] e [TipoDao]) usando um banco de dados
 * em memória. O objetivo é validar se as queries SQL, restrições de chave estrangeira
 * e mapeamentos do Room estão funcionando conforme o esperado antes de testar o repositório.
 */
@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {
    private lateinit var produtoDao: ProdutoDao
    private lateinit var tipoDao: TipoDao
    private lateinit var db: AppDatabase

    /**
     * Cria uma instância temporária do banco de dados em memória antes de cada teste.
     * Bancos de dados em memória são ideais para testes pois são rápidos e os dados
     * são descartados automaticamente quando o processo termina.
     */
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        produtoDao = db.produtoDao()
        tipoDao = db.tipoDao()
    }

    /**
     * Fecha o banco de dados após a execução de cada teste para liberar recursos.
     */
    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    /**
     * Testa a inserção e leitura de um produto.
     * 
     * Nota: Como existe uma restrição de Chave Estrangeira (ForeignKey) na entidade Produto,
     * é necessário inserir um [TipoEntity] correspondente antes de inserir o produto.
     */
    @Test
    @Throws(Exception::class)
    fun writeProdutoAndReadInList() = runBlocking {
        // Primeiro insere um tipo devido à restrição de chave estrangeira em ProdutoEntity
        val tipo = TipoEntity(
            id = 1,
            nome = "Eletrônico",
            imagem = byteArrayOf(0x01)
        )
        tipoDao.insert(tipo)
        
        val produto = ProdutoEntity(
            id = 1,
            tipo = 1,
            marca = "Samsung",
            dataValidade = System.currentTimeMillis(),
            unidade = 1,
            unidadeMedida = "un",
            quantidade = 10
        )
        produtoDao.insert(produto)

        // Recupera a lista diretamente do DAO (chamada suspend)
        val allProdutos = produtoDao.getAll()
        assertEquals(allProdutos[0].marca, "Samsung")
    }

    /**
     * Testa a inserção e leitura de uma categoria (Tipo).
     */
    @Test
    @Throws(Exception::class)
    fun writeTipoAndReadInList() = runBlocking {
        val tipo = TipoEntity(
            id = 1,
            nome = "Smartphones",
            imagem = byteArrayOf(0x01)
        )
        tipoDao.insert(tipo)

        val allTipos = tipoDao.getAll()
        assertEquals(allTipos[0].nome, "Smartphones")
    }
}
