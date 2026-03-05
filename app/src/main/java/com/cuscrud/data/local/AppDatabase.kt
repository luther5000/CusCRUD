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

/**
 * Definição do banco de dados Room para o aplicativo.
 *
 * @Database: Define as entidades (tabelas) que compõem o banco, a versão do esquema
 * e se deve exportar o esquema para um arquivo JSON (útil para migrações).
 *
 * @TypeConverters: Registra conversores para tipos de dados que o SQLite não suporta nativamente
 * (como objetos Date ou tipos complexos personalizados).
 */
@Database(
    entities = [ProdutoEntity::class, TipoEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Funções abstratas para obter os DAOs (Data Access Objects).
     * O Room gera automaticamente a implementação dessas funções.
     */
    abstract fun produtoDao(): ProdutoDao
    abstract fun tipoDao(): TipoDao

    companion object {
        /**
         * A anotação @Volatile garante que o valor de INSTANCE seja sempre atualizado
         * em todas as threads de execução, evitando problemas de cache de memória.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Padrão Singleton para garantir que exista apenas uma instância do banco de dados
         * aberta em todo o aplicativo, economizando recursos computacionais.
         */
        fun getDatabase(context: Context): AppDatabase {
            // Se a INSTANCE não for nula, retorna-a.
            // Se for, inicia um processo sincronizado para criá-la.
            return INSTANCE ?: synchronized(this) {
                // Cria o banco de dados usando o Builder do Room.
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
