package com.cuscrud.data.mapper

import android.util.Base64
import com.cuscrud.data.remote.dto.*
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilitários de mapeamento para converter entre DTOs da API e modelos de domínio.
 */

private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("America/Recife")
}

fun TipoDto.toDomain(): Tipo {
    val rawImage = this.imagem
    val decodedImage = if (rawImage != null && rawImage.contains(",")) {
        val base64Data = rawImage.substringAfter(",")
        try {
            Base64.decode(base64Data, Base64.DEFAULT)
        } catch (e: Exception) {
            byteArrayOf()
        }
    } else if (rawImage != null) {
        try {
            Base64.decode(rawImage, Base64.DEFAULT)
        } catch (e: Exception) {
            byteArrayOf()
        }
    } else {
        byteArrayOf()
    }

    return Tipo(
        id = this.typeId,
        nome = this.nome,
        imagem = decodedImage
    )
}

/**
 * Converte um ProdutoResponseDto para Produto.
 * @param tipoNome Nome opcional do tipo, caso não queira deixá-lo vazio.
 */
fun ProdutoResponseDto.toDomain(tipoNome: String = ""): Produto {
    val date = try {
        this.dataValidade?.let { isoFormat.parse(it) } ?: Date()
    } catch (e: Exception) {
        Date()
    }

    return Produto(
        id = this.productId,
        tipo = Tipo(id = this.typeId, nome = tipoNome, imagem = byteArrayOf()),
        marca = this.marca ?: "",
        dataValidade = date,
        unidade = this.unidade ?: 0L,
        unidadeMedida = this.unidadeMedida ?: "",
        quantidade = this.quantidade
    )
}

fun Produto.toRequestDto(): ProdutoRequestDto {
    return ProdutoRequestDto(
        typeId = this.tipo.id,
        marca = this.marca,
        dataValidade = try { isoFormat.format(this.dataValidade) } catch (e: Exception) { null },
        unidade = this.unidade,
        unidadeMedida = this.unidadeMedida,
        quantidade = this.quantidade
    )
}

fun Produto.toUpdateDto(): ProdutoUpdateDto {
    return ProdutoUpdateDto(
        typeId = this.tipo.id,
        marca = this.marca,
        dataValidade = try { isoFormat.format(this.dataValidade) } catch (e: Exception) { null },
        unidade = this.unidade,
        unidadeMedida = this.unidadeMedida,
        quantidade = this.quantidade
    )
}
