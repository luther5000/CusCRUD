package com.cuscrud.di

import android.content.Context
import com.cuscrud.data.local.AppDatabase
import com.cuscrud.data.local.dao.ProdutoDao
import com.cuscrud.data.local.dao.TipoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo do Hilt responsável por fornecer as dependências relacionadas ao banco de dados Room.
 * 
 * As dependências aqui definidas estarão disponíveis em todo o ciclo de vida do aplicativo,
 * conforme indicado por [SingletonComponent].
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Fornece a instância única (Singleton) do banco de dados [AppDatabase].
     * 
     * O Hilt injeta automaticamente o [ApplicationContext] para inicializar o Room.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * Fornece o DAO para operações relacionadas a produtos.
     * 
     * Depende da instância do [AppDatabase] fornecida neste mesmo módulo.
     */
    @Provides
    fun provideProdutoDao(database: AppDatabase): ProdutoDao {
        return database.produtoDao()
    }

    /**
     * Fornece o DAO para operações relacionadas a categorias (tipos).
     * 
     * Depende da instância do [AppDatabase] fornecida neste mesmo módulo.
     */
    @Provides
    fun provideTipoDao(database: AppDatabase): TipoDao {
        return database.tipoDao()
    }
}
