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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do repositório [AuthRepository] responsável por gerenciar a autenticação e sessão do usuário.
 *
 * Esta classe centraliza a lógica de:
 * - **Login**: Realiza a autenticação, salva o token recebido no [SessionManager] e trata erros específicos de credenciais.
 * - **Registro**: Cria novos usuários no sistema e trata conflitos (como e-mail já em uso).
 * - **Gerenciamento de Sessão**: Provê métodos para logout (limpeza de token) e verificação de estado da sessão.
 * - **Tratamento de Erros**: Converte respostas de erro da API em mensagens amigáveis para a UI.
 *
 * Todas as operações de rede são executadas no [Dispatchers.IO] para garantir que a Main Thread não seja bloqueada.
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
                    sessionManager.saveCredentials(request.login, request.passwd)
                    Result.Success(loginResponse)
                } ?: Result.Error(Exception("Resposta do servidor inválida."))
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Não foi possível se conectar ao servidor.."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao realizar login")
            Result.Error(Exception("Não foi possível realizar o login. Tente novamente mais tarde."))
        }
    }

    override suspend fun register(request: RegisterRequest): Result<RegisterResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error(Exception("Resposta do servidor inválida."))
            } else {
                handleError(response)
            }
        } catch (_: IOException) {
            Result.Error(Exception("Não foi possível se conectar ao servidor."))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao realizar registro")
            Result.Error(Exception("Não foi possível realizar o cadastro no momento."))
        }
    }

    override suspend fun logout() {
        sessionManager.clearAuthToken()
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return !sessionManager.fetchAuthToken().isNullOrBlank()
    }

    private fun handleError(response: Response<*>): Result.Error {
        // Prioridade 1: Mapeamento Estilizado por Contexto (Autenticação)
        val friendlyMessage = when (response.code()) {
            400 -> "Verifique se os dados informados estão corretos."
            401 -> "E-mail ou senha incorretos."
            403 -> "Conta sem permissão de acesso."
            404 -> "Serviço temporariamente indisponível."
            409 -> "Este e-mail já está em uso por outro usuário."
            500 -> "Erro interno no servidor. Tente novamente em instantes."
            else -> null
        }

        if (friendlyMessage != null) {
            return Result.Error(Exception(friendlyMessage))
        }

        // Prioridade 2: Fallback para a mensagem do servidor
        val errorBody = response.errorBody()?.string()
        return try {
            val errorResponse = json.decodeFromString<ErrorResponse>(errorBody ?: "")
            Result.Error(Exception(errorResponse.error.message))
        } catch (e: Exception) {
            Timber.e(e, "Erro ao processar corpo de erro: $errorBody")
            Result.Error(Exception("Erro inesperado (Código: ${response.code()})"))
        }
    }
}
