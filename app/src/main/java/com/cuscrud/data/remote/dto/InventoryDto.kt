package com.cuscrud.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO representando um Inventário retornado pela API.
 */
@Serializable
data class InventoryDto(
    @SerialName("inv_id") val invId: String,
    @SerialName("inv_name") val invName: String,
    val role: Int
)

/**
 * Resposta para a listagem de inventários.
 */
@Serializable
data class InventoryListResponse(
    val inventories: List<InventoryDto>,
    @SerialName("next_page") val nextPage: String? = null
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
