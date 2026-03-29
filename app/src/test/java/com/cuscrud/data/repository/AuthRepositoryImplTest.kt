package com.cuscrud.data.repository

import com.cuscrud.data.local.SessionManager
import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.*
import com.cuscrud.domain.util.Result
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Suite de testes unitários para o [AuthRepositoryImpl].
 * 
 * Esta classe valida o fluxo de autenticação, persistência de tokens e gestão de sessão.
 * Garante que o estado de login do usuário seja refletido corretamente com base na
 * presença de tokens válidos e que erros de rede ou credenciais sejam tratados.
 */
class AuthRepositoryImplTest {

    private lateinit var repository: AuthRepositoryImpl
    private val apiService: CuscrudApiService = mockk()
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        repository = AuthRepositoryImpl(apiService, sessionManager, json)
    }

    // region Login Tests

    /**
     * Objetivo: Validar o sucesso do login.
     * Entradas: LoginRequest válido, API retornando 200 OK com LoginResponse.
     * Critério de Aceitação: Retornar Result.Success com os dados e salvar o token no SessionManager.
     */
    @Test
    fun `login should return Success and save token when API call is successful`() = runTest {
        // Arrange
        val request = LoginRequest("user", "pass")
        val responseDto = LoginResponse(
            token = "jwt_token",
            expiresIn = 3600,
            user = UserDto("1", "User Name", "user")
        )
        coEvery { apiService.login(request) } returns Response.success(responseDto)

        // Act
        val result = repository.login(request)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(responseDto, (result as Result.Success).data)
        coVerify { sessionManager.saveAuthToken("jwt_token") }
    }

    /**
     * Objetivo: Validar erro de autenticação (401 Unauthorized).
     * Entradas: LoginRequest, API retornando 401 com JSON de erro.
     * Critério de Aceitação: Retornar Result.Error com a mensagem da API e não salvar o token.
     */
    @Test
    fun `login should return Error and not save token when API returns 401`() = runTest {
        // Arrange
        val request = LoginRequest("user", "wrong_pass")
        val errorJson = """{"error": {"code": "AUTH_FAILED", "message": "Credenciais inválidas"}}"""
        val errorResponseBody = errorJson.toResponseBody("application/json".toMediaType())
        coEvery { apiService.login(request) } returns Response.error(401, errorResponseBody)

        // Act
        val result = repository.login(request)

        // Assert
        assertTrue(result is Result.Error)
        assertEquals("Credenciais inválidas", (result as Result.Error).exception.message)
        coVerify(exactly = 0) { sessionManager.saveAuthToken(any()) }
    }

    /**
     * Objetivo: Validar falha de rede durante o login.
     * Entradas: LoginRequest, API lançando IOException.
     * Critério de Aceitação: Retornar Result.Error com mensagem amigável de conexão.
     */
    @Test
    fun `login should return Error when network exception occurs`() = runTest {
        // Arrange
        val request = LoginRequest("user", "pass")
        coEvery { apiService.login(request) } throws IOException("No internet")

        // Act
        val result = repository.login(request)

        // Assert
        assertTrue(result is Result.Error)
        assertEquals("Falha de conexão. Verifique sua internet.", (result as Result.Error).exception.message)
    }

    // endregion

    // region Logout Tests

    /**
     * Objetivo: Validar o encerramento da sessão.
     * Entradas: Chamada para logout().
     * Critério de Aceitação: Invocar clearAuthToken no SessionManager para remover credenciais.
     */
    @Test
    fun `logout should clear token from session manager`() = runTest {
        // Act
        repository.logout()

        // Assert
        coVerify { sessionManager.clearAuthToken() }
    }

    // endregion

    // region Session Tests

    /**
     * Objetivo: Verificar se o sistema identifica corretamente um usuário logado.
     * Entradas: SessionManager possuindo um token não nulo.
     * Critério de Aceitação: Retornar true.
     */
    @Test
    fun `isUserLoggedIn should return true when token exists`() = runTest {
        // Arrange
        coEvery { sessionManager.fetchAuthToken() } returns "valid_token"

        // Act
        val result = repository.isUserLoggedIn()

        // Assert
        assertTrue(result)
    }

    /**
     * Objetivo: Verificar se o sistema identifica corretamente um usuário não logado.
     * Entradas: SessionManager retornando nulo para o token.
     * Critério de Aceitação: Retornar false.
     */
    @Test
    fun `isUserLoggedIn should return false when token is null`() = runTest {
        // Arrange
        coEvery { sessionManager.fetchAuthToken() } returns null

        // Act
        val result = repository.isUserLoggedIn()

        // Assert
        assertFalse(result)
    }

    // endregion
}
