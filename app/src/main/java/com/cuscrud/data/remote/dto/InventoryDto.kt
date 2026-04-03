package com.cuscrud.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Este arquivo contém os Data Transfer Objects (DTOs) para o gerenciamento de inventários.
 * Ele define a estrutura de dados para:
 * - Representação de um inventário (InventoryDto).
 * - Listagem de inventários (InventoryListResponse).
 * - Criação e atualização de inventários (Create/UpdateInventoryRequest).
 *
 * Utiliza kotlinx.serialization para a comunicação com a API remota.
 */


/**
 * DTO representando um Inventário retornado pela API.
 */
@Serializable
data class InventoryDto(
    @SerialName("inv_id") val invId: String,
    @SerialName("inv_name") val invName: String,
    val role: Int? = null
)

/**
 * Resposta para a listagem de inventários (Seção 5.2.4).
 */
@Serializable
data class InventoryListResponse(
    val inventories: List<InventoryDto>,
    @SerialName("next_page") val nextPage: String? = null
)

/**
 * Resposta para criação e edição de inventário (Seções 5.2.1 e 5.2.2).
 */
@Serializable
data class InventoryResponse(
    val inventory: InventoryDto,
    val role: Int
)

/**
 * Request para criação de um novo inventário.
 */
@Serializable
data class CreateInventoryRequest(
    @SerialName("inv_name") val invName: String
)

/**
 * Request para atualização de um inventário existente.
 */
@Serializable
data class UpdateInventoryRequest(
    @SerialName("inv_name") val invName: String
)
