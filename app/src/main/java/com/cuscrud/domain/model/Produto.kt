package com.cuscrud.domain.model

import java.util.Date

/**
 * Modelos de negócio representando um produto.
 * Essa classe é utilizada pelo backend e UI.
 */
data class Produto(
    val id: Long,
    val tipo: Tipo,
    val marca: String,
    val dataValidade: Date,
    val unidade: Long,
    val unidadeMedida: String,
    val quantidade: Long
)
