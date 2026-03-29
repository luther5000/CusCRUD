package com.cuscrud.data.remote.interceptors

import com.cuscrud.data.local.SessionManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var authInterceptor: AuthInterceptor
    private val sessionManager: SessionManager = mockk()
    private val chain: Interceptor.Chain = mockk()

    @Before
    fun setup() {
        authInterceptor = AuthInterceptor(sessionManager)
    }

    // region Interceptor Header Injection Tests

    /**
     * Objetivo: Verificar se o interceptor injeta o cabeçalho Authorization quando existe um token.
     * Entradas: Mock do SessionManager retornando "valid_token" e um request original sem headers.
     * Comportamento esperado: O chain.proceed deve ser chamado com um novo request contendo "Authorization: Bearer valid_token".
     */
    @Test
    fun `intercept should add Authorization header when token exists`() = runTest {
        // Arrange
        val token = "valid_token"
        val originalRequest = Request.Builder()
            .url("https://api.example.com/")
            .build()
        
        // fetchAuthToken agora é suspend, usamos coEvery
        coEvery { sessionManager.fetchAuthToken() } returns token
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        // Act
        authInterceptor.intercept(chain)

        // Assert
        verify {
            chain.proceed(withArg { request ->
                assertEquals("Bearer $token", request.header("Authorization"))
            })
        }
    }

    /**
     * Objetivo: Verificar se o interceptor não injeta o cabeçalho Authorization quando não existe um token.
     * Entradas: Mock do SessionManager retornando null e um request original sem headers.
     * Comportamento esperado: O chain.proceed deve ser chamado com um request idêntico ao original (sem header Authorization).
     */
    @Test
    fun `intercept should not add Authorization header when token is null`() = runTest {
        // Arrange
        val originalRequest = Request.Builder()
            .url("https://api.example.com/")
            .build()
        
        coEvery { sessionManager.fetchAuthToken() } returns null
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        // Act
        authInterceptor.intercept(chain)

        // Assert
        verify {
            chain.proceed(withArg { request ->
                assertEquals(null, request.header("Authorization"))
            })
        }
    }

    // endregion
}
