package com.cuscrud.domain.repository

import com.cuscrud.data.remote.dto.*
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface que define as operações de autenticação e gerenciamento de sessão.
 */
interface AuthRepository {
    /**
     * Realiza a autenticação do usuário.
     * @param request Dados de login.
     * @return Flow emitindo o resultado da operação.
     */
    suspend fun login(request: LoginRequest): Result<LoginResponse>

    /**
     * Registra um novo usuário no sistema.
     * @param request Dados para registro.
     * @return Flow emitindo o resultado da operação.
     */
    suspend fun register(request: RegisterRequest): Result<RegisterResponse>

    /**
     * Encerra a sessão do usuário atual, limpando tokens e dados locais.
     */
    suspend fun logout()

    /**
     * Verifica se existe uma sessão ativa (token presente).
     */
    fun isUserLoggedIn(): Boolean
}
