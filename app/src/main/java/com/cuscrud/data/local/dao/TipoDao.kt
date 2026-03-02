package com.cuscrud.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cuscrud.data.local.entities.TipoEntity

@Dao
interface TipoDao {
    @Query("SELECT * FROM tipo")
    fun getAll(): List<TipoEntity>

    @Insert
    suspend fun insert(tipo: TipoEntity)
}