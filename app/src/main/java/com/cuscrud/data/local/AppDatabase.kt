package com.cuscrud.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cuscrud.data.local.converters.Converters
import com.cuscrud.data.local.dao.ProdutoDao
import com.cuscrud.data.local.dao.TipoDao
import com.cuscrud.data.local.entities.ProdutoEntity
import com.cuscrud.data.local.entities.TipoEntity

@Database(
    entities = [ProdutoEntity::class, TipoEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produtoDao(): ProdutoDao
    abstract fun tipoDao(): TipoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meu_estoque_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}