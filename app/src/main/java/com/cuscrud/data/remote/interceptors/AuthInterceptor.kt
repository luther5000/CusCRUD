package com.cuscrud.data.remote.interceptors

import com.cuscrud.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor para injetar o token de autenticação Bearer em todas as requisições.
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
