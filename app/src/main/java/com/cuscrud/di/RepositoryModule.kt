package com.cuscrud.di

import com.cuscrud.data.repository.OfflineProdutoRepository
import com.cuscrud.data.repository.OfflineTipoRepository
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.repository.TipoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * NOTA SOBRE O USO:
 * As funções neste módulo podem aparecer como "não utilizadas" (em cinza) no Android Studio.
 * Isso ocorre porque elas não são chamadas diretamente pelo nosso código, mas sim pelo código
 * gerado pelo Hilt/Dagger durante a compilação. Elas são ESSENCIAIS para que o Hilt saiba
 * como instanciar as interfaces dos repositórios quando solicitadas nos ViewModels.
 * Se removidas, o projeto apresentará erro de compilação por falta de "bindings".
 */

/**
 * Módulo do Hilt encarregado de vincular (bind) as interfaces de repositório às suas
 * implementações concretas (offline).
 * 
 * O uso de uma classe abstrata com @Binds é uma prática recomendada para injetar
 * interfaces, sendo mais eficiente que o @Provides.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Vincula a interface [ProdutoRepository] à implementação [OfflineProdutoRepository].
     * 
     * Isso permite que os ViewModels dependam da interface, facilitando a troca de
     * implementação (ex: para uma versão online ou mock) sem alterar o código do consumidor.
     */
    @Binds
    @Singleton
    abstract fun bindProdutoRepository(
        offlineProdutoRepository: OfflineProdutoRepository
    ): ProdutoRepository

    /**
     * Vincula a interface [TipoRepository] à implementação [OfflineTipoRepository].
     * 
     * Mantém o padrão de injeção de dependência baseado em interfaces para as categorias.
     */
    @Binds
    @Singleton
    abstract fun bindTipoRepository(
        offlineTipoRepository: OfflineTipoRepository
    ): TipoRepository
}
