package com.cuscrud.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecureStorage

// Delegate padrão do AndroidX DataStore que garante singleton por processo
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

/**
 * Módulo responsável por prover instâncias de armazenamento local usando DataStore.
 */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @SecureStorage
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.sessionDataStore
    }
}
