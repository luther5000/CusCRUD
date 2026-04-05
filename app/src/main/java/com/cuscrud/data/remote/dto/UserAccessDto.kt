package com.cuscrud.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Este arquivo define os Data Transfer Objects (DTOs) para o controle de acesso baseado em funções (RBAC) nos inventários.
 * Ele gerencia a estrutura de dados para:
 * - **UserAccessDto**: Detalhes de um usuário e seu nível de permissão (role) em um inventário.
 * - **UserAccessListResponse**: Resposta paginada contendo a lista de usuários vinculados a um inventário específico.
 * - **AddUserAccessRequest**: Requisição para convidar/adicionar um novo usuário a um inventário com uma função definida.
 * - **UpdateUserAccessRequest**: Requisição para alterar o nível de acesso (role) de um usuário já existente no inventário.
 *
 * Utiliza kotlinx.serialization para garantir a integridade dos dados trafegados via JSON.
 */

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
 * Resposta para adição e atualização de colaborador (Seções 5.3.2 e 5.3.3).
 */
@Serializable
data class UserAccessResponse(
    val inventory: InventoryDto,
    val user: UserAccessDto
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
