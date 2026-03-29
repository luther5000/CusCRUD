package com.cuscrud.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cuscrud.data.local.entities.TipoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TipoDao {
    @Query("SELECT * FROM tipo")
    fun getAll(): List<TipoEntity>

    @Query("SELECT * FROM tipo")
    fun getAllFlow(): Flow<List<TipoEntity>>

    @Query("SELECT * FROM tipo WHERE id = :id")
    suspend fun getById(id: Long): TipoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tipo: TipoEntity): Long

    @Update
    suspend fun update(tipo: TipoEntity)

    @Delete
    suspend fun delete(tipo: TipoEntity)
}
