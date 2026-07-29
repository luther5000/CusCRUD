package com.cuscrud.data.remote.interceptors

import com.cuscrud.data.local.SessionManager
import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.LoginRequest
import com.cuscrud.data.remote.dto.LoginResponse
import com.cuscrud.data.remote.dto.UserDto
import io.mockk.*
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

/**
 * Suite de testes unitários para o [TokenAuthenticator].
 * 
 * Esta classe valida o fluxo de "Silent Login" acionado por erros 401 (Unauthorized) do OkHttp.
 * Os testes garantem que o sistema tente renovar o token automaticamente usando credenciais salvas
 * e trate corretamente tanto o sucesso quanto falhas na reautenticação.
 */
class TokenAuthenticatorTest {

    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val apiService: CuscrudApiService = mockk()
    private val apiServiceProvider: Provider<CuscrudApiService> = mockk()
    
    private lateinit var authenticator: TokenAuthenticator

    @Before
    fun setup() {
        every { apiServiceProvider.get() } returns apiService
        authenticator = TokenAuthenticator(sessionManager, apiServiceProvider)
    }

    /**
     * Objetivo: Ignorar erros que não sejam de falta de autorização.
     * Entradas: Resposta HTTP com código 400 (Bad Request).
     * Critério de Aceitação: O método authenticate deve retornar null, não tentando realizar login.
     */
    @Test
    fun `authenticate should return null when response code is not 401`() {
        val request = Request.Builder().url("https://api.test.com").build()
        val response = createResponse(request, 400)

        val result = authenticator.authenticate(null, response)

        assertNull(result)
        verify(exactly = 0) { runBlocking { sessionManager.fetchCredentials() } }
    }

    /**
     * Objetivo: Validar comportamento quando o erro é 401 mas não há credenciais salvas.
     * Entradas: Resposta HTTP 401 e SessionManager retornando null para credenciais.
     * Critério de Aceitação: Retornar null, indicando que o Silent Login não é possível.
     */
    @Test
    fun `authenticate should return null when error is 401 but no credentials exist`() {
        val request = Request.Builder().url("https://api.test.com").build()
        val response = createResponse(request, 401)
        coEvery { sessionManager.fetchCredentials() } returns null

        val result = authenticator.authenticate(null, response)

        assertNull(result)
    }

    /**
     * Objetivo: Validar o fluxo de falha no Silent Login (ex: senha alterada).
     * Entradas: Resposta HTTP 401, credenciais salvas presentes, mas API de login retornando erro.
     * Critério de Aceitação: O sistema deve limpar a sessão (logout) e retornar null.
     */
    @Test
    fun `authenticate should clear session and return null when silent login fails`() {
        val request = Request.Builder().url("https://api.test.com").build()
        val response = createResponse(request, 401)
        coEvery { sessionManager.fetchCredentials() } returns ("user" to "wrong_pass")
        
        // Simula falha na chamada de login (ex: 401 na reautenticação)
        coEvery { apiService.login(any()) } returns retrofit2.Response.error(401, mockk(relaxed = true))

        val result = authenticator.authenticate(null, response)

        assertNull(result)
        coVerify { sessionManager.clearAuthToken() }
    }

    /**
     * Objetivo: Validar o sucesso do Silent Login e renovação da requisição original.
     * Entradas: Resposta HTTP 401, credenciais salvas presentes e API de login retornando sucesso com novo token.
     * Critério de Aceitação: O novo token deve ser salvo e uma nova requisição com o header 
     * Authorization atualizado deve ser retornada.
     */
    @Test
    fun `authenticate should save new token and return updated request when silent login succeeds`() {
        val request = Request.Builder().url("https://api.test.com").build()
        val response = createResponse(request, 401)
        val login = "user"
        val pass = "pass"
        val newToken = "new_jwt_token"
        
        coEvery { sessionManager.fetchCredentials() } returns (login to pass)
        
        val loginResponseDto = LoginResponse(
            token = newToken,
            expiresIn = 3600,
            user = UserDto("1", "User", login)
        )
        coEvery { apiService.login(LoginRequest(login, pass)) } returns retrofit2.Response.success(loginResponseDto)

        val result = authenticator.authenticate(null, response)

        // Verificações
        coVerify { sessionManager.saveAuthToken(newToken) }
        assertEquals("Bearer $newToken", result?.header("Authorization"))
        assertEquals(request.url, result?.url)
    }

    private fun createResponse(request: Request, code: Int): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Response Message")
            .build()
    }
}
