package com.cuscrud.di

import com.cuscrud.data.repository.*
import com.cuscrud.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(
        inventoryRepositoryImpl: InventoryRepositoryImpl
    ): InventoryRepository

    @Binds
    @Singleton
    abstract fun bindAccessRepository(
        accessRepositoryImpl: AccessRepositoryImpl
    ): AccessRepository

    @Binds
    @Singleton
    abstract fun bindProdutoRepository(
        remoteProdutoRepository: RemoteProdutoRepository
    ): ProdutoRepository

    @Binds
    @Singleton
    abstract fun bindTipoRepository(
        remoteTipoRepository: RemoteTipoRepository
    ): TipoRepository
}