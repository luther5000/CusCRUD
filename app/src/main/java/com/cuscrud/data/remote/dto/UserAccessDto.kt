package com.cuscrud.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO representando o acesso de um usuário a um inventário.
 */
@Serializable
data class UserAccessDto(
    @SerialName("user_id") val userId: String,
    val name: String,
    val login: String,
    val role: Int
)

/**
 * Resposta para a listagem de usuários com acesso a um inventário.
 */
@Serializable
data class UserAccessListResponse(
    val inventory: InventoryDto,
    val users: List<UserAccessDto>,
    @SerialName("next_page") val nextPage: String? = null
)

/**
 * Request para adicionar um novo usuário ao inventário.
 */
@Serializable
data class AddUserAccessRequest(
    val login: String,
    val role: Int
)

/**
 * Request para atualizar o papel (role) de um usuário no inventário.
 */
@Serializable
data class UpdateUserAccessRequest(
    val role: Int
)
