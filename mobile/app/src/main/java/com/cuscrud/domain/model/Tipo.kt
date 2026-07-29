package com.cuscrud.domain.model

/**
 * Modelos de negócio representando um tipo de produto.
 * Essa classe é utilizada pelo backend e UI.
 */
data class Tipo(
    val id: Long,
    val nome: String,
    val imagem: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tipo

        if (id != other.id) return false
        if (nome != other.nome) return false
        if (!imagem.contentEquals(other.imagem)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + nome.hashCode()
        result = 31 * result + imagem.contentHashCode()
        return result
    }
}
