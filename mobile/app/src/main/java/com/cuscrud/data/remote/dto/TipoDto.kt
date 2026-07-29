package com.cuscrud.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Este arquivo define os Data Transfer Objects (DTOs) para o gerenciamento de categorias/tipos de produtos.
 * As classes aqui presentes facilitam a comunicação com a API para:
 * - **Representação (TipoDto)**: Dados detalhados de um tipo, incluindo suporte a imagens em Base64.
 * - **Listagem (TipoListResponse)**: Resposta paginada contendo uma coleção de tipos.
 * - **Criação e Atualização (Create/UpdateTipoRequest)**: Estruturas para envio de novos dados ou modificações parciais.
 *
 * O uso de Base64 para imagens permite o transporte de mídia diretamente no corpo do JSON.
 * Utiliza kotlinx.serialization para a conversão de dados.
 */

/**
 * DTO para representação de um Tipo de produto retornado pela API.
 */
@Serializable
data class TipoDto(
    @SerialName("type_id") val typeId: Long,
    @SerialName("nome") val nome: String,
    @SerialName("has_image") val hasImage: Boolean? = null,
    @SerialName("imagem") val imagem: String? = null, // Base64
    @SerialName("inv_id") val invId: String? = null
)

/**
 * DTO para resposta de listagem paginada de Tipos.
 */
@Serializable
data class TipoListResponse(
    @SerialName("types") val types: List<TipoDto>,
    @SerialName("next_page") val nextPage: String? = null
)

/**
 * DTO para requisição de criação de um novo Tipo.
 */
@Serializable
data class CreateTipoRequest(
    @SerialName("nome") val nome: String,
    @SerialName("imagem") val imagem: String? = null // data:<mime>;base64,<dados>
)

/**
 * DTO para requisição de atualização parcial de um Tipo.
 */
@Serializable
data class UpdateTipoRequest(
    @SerialName("nome") val nome: String? = null,
    @SerialName("imagem") val imagem: String? = null // data:<mime>;base64,<dados> ou null para remover
)
