package com.cuscrud.testutil

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import java.util.Date

/**
 * Utilitário para geração de dados de teste.
 *
 * Fornece métodos auxiliares para criar instâncias de modelos de domínio ([Produto] e [Tipo])
 * com valores padrão, facilitando a escrita de testes e reduzindo a duplicação de código.
 */
object TestDataGenerator {

    /**
     * Cria uma instância de [Tipo] para uso em testes.
     *
     * @param id Identificador único da categoria.
     * @param nome Nome da categoria.
     * @param imagem Array de bytes representando a imagem da categoria.
     * @return Uma instância de [Tipo] populada.
     */
    fun createTipo(
        id: Long = 1L,
        nome: String = "Eletrônicos",
        imagem: ByteArray = byteArrayOf(0x01)
    ): Tipo = Tipo(id, nome, imagem)

    /**
     * Cria uma instância de [Produto] para uso em testes.
     *
     * @param id Identificador único do produto.
     * @param tipo Objeto [Tipo] associado ao produto.
     * @param marca Marca do produto.
     * @param dataValidade Data de validade do produto.
     * @param unidade Quantidade por embalagem ou unidade.
     * @param unidadeMedida Sigla da unidade de medida (ex: un, kg, ml).
     * @param quantidade Quantidade total em estoque.
     * @return Uma instância de [Produto] populada.
     */
    fun createProduto(
        id: Long = 1L,
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
