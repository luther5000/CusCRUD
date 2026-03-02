package com.cuscrud.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cuscrud.data.local.entities.ProdutoEntity

@Dao
interface ProdutoDao {
    @Query("SELECT * FROM produto")
    fun getAll(): List<ProdutoEntity>

    @Insert
    suspend fun insert(produto: ProdutoEntity)
}