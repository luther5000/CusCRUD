package com.cuscrud.di

import com.cuscrud.BuildConfig
import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.interceptors.AuthInterceptor
import com.cuscrud.data.remote.interceptors.TokenAuthenticator
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Módulo de Injeção de Dependências (Hilt) responsável pela configuração da camada de rede.
 *
 * Este módulo provê as instâncias necessárias para a comunicação com a API REST:
 * - **Json**: Configuração do serializador Kotlinx Serialization, ignorando chaves desconhecidas.
 * - **OkHttpClient**: Cliente HTTP configurado com o [AuthInterceptor] para gerenciamento automático de tokens
 *   e [TokenAuthenticator] para Silent Login (renovação automática em caso de 401).
 * - **Retrofit**: Cliente Type-safe configurado com a Base URL do projeto e conversor JSON.
 * - **CuscrudApiService**: Interface que define os contratos de endpoints da aplicação.
 *
 * Todas as dependências são providas como `@Singleton` para garantir uma única instância em todo o ciclo de vida do app.
 */

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideCuscrudApiService(retrofit: Retrofit): CuscrudApiService {
        return retrofit.create(CuscrudApiService::class.java)
    }
}
