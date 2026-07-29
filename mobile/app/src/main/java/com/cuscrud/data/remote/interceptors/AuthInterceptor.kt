package com.cuscrud.data.remote.interceptors

import com.cuscrud.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Este interceptor é responsável por gerenciar a autenticação das requisições de rede.
 * Ele atua de forma centralizada para:
 * - **Injeção de Token**: Recupera o token de autenticação atual através do [SessionManager].
 * - **Autorização**: Adiciona automaticamente o cabeçalho `Authorization: Bearer <token>` em todas as chamadas de saída da API,
 *   garantindo que o usuário permaneça autenticado perante o servidor.
 * - **Sincronização**: Utiliza `runBlocking` para obter o token de forma segura dentro do fluxo síncrono do OkHttp.
 *
 * Sendo um `@Singleton`, ele mantém a consistência da sessão em toda a aplicação.
 */

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Usamos runBlocking aqui porque o OkHttp executa interceptores em threads de rede (background)
        // e precisamos do token de forma síncrona para prosseguir com a requisição.
        val token = runBlocking { sessionManager.fetchAuthToken() }

        val request = chain.request().newBuilder()

        if (!token.isNullOrBlank()) {
            request.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(request.build())
    }
}
