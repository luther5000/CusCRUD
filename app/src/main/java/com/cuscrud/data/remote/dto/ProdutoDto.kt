package com.cuscrud.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProdutoRequestDto(
    @SerialName("type_id") val typeId: Long,
    val marca: String,
    val dataValidade: String, // ISO 8601
    val unidade: Long,
    val unidadeMedida: String,
    val quantidade: Long
)

/**
 * DTO para atualizações parciais (PATCH).
 * Todos os campos são opcionais.
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

@Serializable
data class ProdutoResponseDto(
    val id: Int,
    val type: TipoResponseDto,
    val marca: String,
    @SerialName("data_validade") val dataValidade: String,
    val unidade: Long,
    @SerialName("unidade_medida") val unidadeMedida: String,
    val quantidade: Long
)

@Serializable
data class TipoResponseDto(
    val id: Long,
    val nome: String,
    val imagem: String // Base64
)
