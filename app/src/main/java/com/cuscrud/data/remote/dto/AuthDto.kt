package com.cuscrud.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
