package com.cuscrud.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cuscrud.data.local.entities.ProdutoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdutoDao {
    @Query("SELECT * FROM produto")
    suspend fun getAll(): List<ProdutoEntity>

    @Query("SELECT * FROM produto")
    fun getAllFlow(): Flow<List<ProdutoEntity>>

    @Query("SELECT * FROM produto WHERE id = :id")
    suspend fun getById(id: Long): ProdutoEntity?

    @Query("SELECT * FROM produto WHERE tipo = :tipoId")
    fun getByTipo(tipoId: Long): Flow<List<ProdutoEntity>>

    @Query("SELECT * FROM produto WHERE tipo = :tipoId")
    suspend fun getByTipoSync(tipoId: Long): List<ProdutoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(produto: ProdutoEntity)

    @Update
    suspend fun update(produto: ProdutoEntity)

    @Delete
    suspend fun delete(produto: ProdutoEntity)
}
