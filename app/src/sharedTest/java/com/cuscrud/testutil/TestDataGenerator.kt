package com.cuscrud.testutil

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import java.util.Date

object TestDataGenerator {

    fun createTipo(
        id: Long = 1L,
        nome: String = "Eletrônicos",
        imagem: ByteArray = byteArrayOf(0x01)
    ): Tipo = Tipo(id, nome, imagem)

    fun createProduto(
        id: Int = 1,
        tipo: Tipo = createTipo(),
        marca: String = "Samsung",
        dataValidade: Date = Date(),
        unidade: Long = 1,
        unidadeMedida: String = "un",
        quantidade: Long = 10
    ): Produto = Produto(
        id = id,
        tipo = tipo,
        marca = marca,
        dataValidade = dataValidade,
        unidade = unidade,
        unidadeMedida = unidadeMedida,
        quantidade = quantidade
    )
}
