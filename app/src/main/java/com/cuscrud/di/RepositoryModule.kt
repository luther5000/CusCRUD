package com.cuscrud.di

import com.cuscrud.data.repository.AuthRepositoryImpl
import com.cuscrud.data.repository.InventoryRepositoryImpl
import com.cuscrud.data.repository.OfflineTipoRepository
import com.cuscrud.data.repository.RemoteProdutoRepository
import com.cuscrud.domain.repository.AuthRepository
import com.cuscrud.domain.repository.InventoryRepository
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.repository.TipoRepository
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
    abstract fun bindProdutoRepository(
        remoteProdutoRepository: RemoteProdutoRepository
    ): ProdutoRepository

    @Binds
    @Singleton
    abstract fun bindTipoRepository(
        offlineTipoRepository: OfflineTipoRepository
    ): TipoRepository
}
