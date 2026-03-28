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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

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
     * Comportamento esperado: Retornar Result.Success com os dados e salvar o token no SessionManager.
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
        verify { sessionManager.saveAuthToken("jwt_token") }
    }

    /**
     * Objetivo: Validar erro de autenticação (401 Unauthorized).
     * Entradas: LoginRequest, API retornando 401 com JSON de erro.
     * Comportamento esperado: Retornar Result.Error com a mensagem da API e não salvar o token.
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
        verify(exactly = 0) { sessionManager.saveAuthToken(any()) }
    }

    /**
     * Objetivo: Validar falha de rede durante o login.
     * Entradas: LoginRequest, API lançando IOException.
     * Comportamento esperado: Retornar Result.Error capturando a exceção.
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
        assertTrue((result as Result.Error).exception is IOException)
    }

    // endregion

    // region Logout Tests

    /**
     * Objetivo: Validar o encerramento da sessão.
     * Entradas: Chamada para logout().
     * Comportamento esperado: Invocar clearAuthToken no SessionManager.
     */
    @Test
    fun `logout should clear token from session manager`() = runTest {
        // Act
        repository.logout()

        // Assert
        verify { sessionManager.clearAuthToken() }
    }

    // endregion
}
