package com.cuscrud.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Este arquivo define os Data Transfer Objects (DTOs) utilizados para as operações de autenticação no sistema.
 * As classes aqui presentes mapeiam a estrutura de dados das requisições e respostas da API para:
 * - **Login**: Credenciais e retorno de token de acesso.
 * - **Registro**: Criação de novos usuários e confirmação de cadastro.
 * - **UserDto**: Representação simplificada do usuário no sistema.
 * - **ErrorResponse**: Estrutura padrão para tratamento de erros retornados pelo servidor.
 *
 * Todas as classes utilizam `kotlinx.serialization` para garantir a correta serialização/desserialização do JSON.
 */
@Serializable
data class LoginRequest(
    val login: String,
    val passwd: String
)

@Serializable
data class LoginResponse(
    val token: String,
    @SerialName("expires_in") val expiresIn: Long,
    val user: UserDto
)

@Serializable
data class RegisterRequest(
    val name: String,
    val login: String,
    val passwd: String
)

@Serializable
data class RegisterResponse(
    @SerialName("user_id") val userId: String,
    val name: String,
    val login: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val login: String
)

@Serializable
data class ErrorResponse(
    val error: ErrorDetails
)

@Serializable
data class ErrorDetails(
    val code: String,
    val message: String
)
