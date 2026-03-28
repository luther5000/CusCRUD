package com.cuscrud.data.repository

import com.cuscrud.data.local.SessionManager
import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.*
import com.cuscrud.domain.repository.AuthRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do repositório de autenticação que interage com a API remota e o armazenamento local seguro.
 *
 * @property apiService Interface do Retrofit para chamadas de rede.
 * @property sessionManager Gerenciador de sessão segura para persistência do token.
 * @property json Instância do Kotlinx Serialization para parse de erros.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: CuscrudApiService,
    private val sessionManager: SessionManager,
    private val json: Json
) : AuthRepository {

    override suspend fun login(request: LoginRequest): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(request)
            if (response.isSuccessful) {
                response.body()?.let { loginResponse ->
                    sessionManager.saveAuthToken(loginResponse.token)
                    Result.Success(loginResponse)
                } ?: Result.Error(Exception("Corpo da resposta de login vazio"))
            } else {
                handleError(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao realizar login")
            Result.Error(e)
        }
    }

    override suspend fun register(request: RegisterRequest): Result<RegisterResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("Corpo da resposta de registro vazio"))
            } else {
                handleError(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao realizar registro")
            Result.Error(e)
        }
    }

    override suspend fun logout() {
        sessionManager.clearAuthToken()
    }

    override fun isUserLoggedIn(): Boolean {
        return !sessionManager.fetchAuthToken().isNullOrBlank()
    }

    /**
     * Realiza o parse de erros vindos da API seguindo o padrão { "error": { "code": "...", "message": "..." } }.
     */
    private fun handleError(response: Response<*>): Result.Error {
        val errorBody = response.errorBody()?.string()
        return try {
            val errorResponse = json.decodeFromString<ErrorResponse>(errorBody ?: "")
            Result.Error(Exception(errorResponse.error.message))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao processar corpo de erro: $errorBody")
            Result.Error(Exception("Ocorreu um erro inesperado: ${response.code()}"))
        }
    }
}
