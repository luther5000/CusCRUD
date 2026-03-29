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

/**
 * Suite de testes unitários para o [AuthInterceptor].
 * 
 * Esta classe valida a lógica de interceptação de requisições HTTP para injeção automática
 * de tokens de autenticação (JWT). Garante que o cabeçalho "Authorization" seja adicionado
 * apenas quando houver uma sessão ativa, mantendo a transparência para o restante da app.
 */
class AuthInterceptorTest {

    private lateinit var authInterceptor: AuthInterceptor
    private val sessionManager: SessionManager = mockk()
    private val chain: Interceptor.Chain = mockk()

    @Before
    fun setup() {
        authInterceptor = AuthInterceptor(sessionManager)
    }

    // region Bloco: Injeção de Cabeçalhos

    /**
     * Objetivo: Verificar a injeção do token JWT nas requisições de saída.
     * Entradas: Mock do SessionManager retornando um token válido e uma requisição original.
     * Critério de Aceitação: A requisição processada pelo 'chain' deve conter o header 
     * 'Authorization' no formato 'Bearer <token>'.
     */
    @Test
    fun `intercept should add Authorization header when token exists`() = runTest {
        // Arrange
        val token = "valid_token"
        val originalRequest = Request.Builder()
            .url("https://api.example.com/")
            .build()
        
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
     * Objetivo: Evitar o envio de cabeçalhos de autorização inválidos/vazios.
     * Entradas: SessionManager sem token armazenado (null).
     * Critério de Aceitação: A requisição deve prosseguir sem o header 'Authorization',
     * permitindo que requisições públicas funcionem normalmente.
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
