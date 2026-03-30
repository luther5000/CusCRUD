package com.cuscrud.data.remote.interceptors

import com.cuscrud.data.local.SessionManager
import com.cuscrud.data.remote.api.CuscrudApiService
import com.cuscrud.data.remote.dto.LoginRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * [TokenAuthenticator] implementa a interface [Authenticator] do OkHttp para gerenciar a renovação
 * automática de tokens JWT quando o servidor retorna um erro 401 (Unauthorized).
 *
 * Esta classe realiza o "Silent Login":
 * 1. Recupera as credenciais criptografadas do [SessionManager].
 * 2. Tenta realizar um novo login de forma síncrona.
 * 3. Se obtiver sucesso, salva o novo token e repete a requisição original com o novo header.
 * 4. Se falhar (ex: senha alterada), limpa a sessão para forçar novo login manual.
 *
 * Utilizamos [Provider] para a [CuscrudApiService] para evitar dependência circular durante a
 * inicialização do Hilt, já que a API depende do OkHttpClient que por sua vez depende deste autenticador.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiServiceProvider: Provider<CuscrudApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 1. Verificamos se o erro é realmente 401
        if (response.code != 401) return null

        // 2. Recuperamos as credenciais salvas
        val credentials = runBlocking { sessionManager.fetchCredentials() } ?: return null
        val (login, password) = credentials

        synchronized(this) {
            // 3. Tentamos o login silencioso
            // Precisamos rodar bloqueando a thread atual do OkHttp
            val loginResponse = runBlocking {
                try {
                    apiServiceProvider.get().login(LoginRequest(login, password))
                } catch (e: Exception) {
                    null
                }
            }

            return if (loginResponse?.isSuccessful == true) {
                val newToken = loginResponse.body()?.token
                if (newToken != null) {
                    // 4. Sucesso: Salva o novo token
                    runBlocking { sessionManager.saveAuthToken(newToken) }

                    // 5. Retorna a requisição original clonada com o novo token
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                } else {
                    handleLogout()
                    null
                }
            } else {
                // 6. Falha no login silencioso (ex: credenciais inválidas)
                handleLogout()
                null
            }
        }
    }

    private fun handleLogout() {
        runBlocking {
            sessionManager.clearAuthToken()
        }
    }
}
