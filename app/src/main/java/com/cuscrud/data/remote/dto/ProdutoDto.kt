package com.cuscrud.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para requisição de criação de produto (Seção 5.5.4)
 */
@Serializable
data class ProdutoRequestDto(
    @SerialName("type_id") val typeId: Long,
    val marca: String? = null,
    val dataValidade: String? = null, // ISO 8601
    val unidade: Long? = null,
    val unidadeMedida: String? = null,
    val quantidade: Long = 0
)

/**
 * DTO para resposta de produto (Seção 5.5.1 / 5.5.2)
 */
@Serializable
data class ProdutoResponseDto(
    @SerialName("product_id") val productId: Long,
    @SerialName("type_id") val typeId: Long,
    val marca: String? = null,
    val dataValidade: String? = null,
    val unidade: Long? = null,
    val unidadeMedida: String? = null,
    val quantidade: Long,
    @SerialName("inv_id") val invId: String
)

/**
 * DTO para resposta de listagem de produtos (Seção 5.5.1)
 */
@Serializable
data class ProdutoListResponse(
    val products: List<ProdutoResponseDto>,
    @SerialName("next_page") val nextPage: String? = null
)

/**
 * DTO para atualização parcial (Seção 5.5.5)
 */
@Serializable
data class ProdutoUpdateDto(
    @SerialName("type_id") val typeId: Long? = null,
    val marca: String? = null,
    val dataValidade: String? = null,
    val unidade: Long? = null,
    val unidadeMedida: String? = null,
    val quantidade: Long? = null
)
