package com.cuscrud.data.mapper

import android.util.Base64
import com.cuscrud.data.local.entities.ProdutoEntity
import com.cuscrud.data.local.entities.TipoEntity
import com.cuscrud.data.remote.dto.*
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import java.text.SimpleDateFormat
import java.util.*

/**
 * Formato ISO 8601 conforme Seção 3.12 da architecture.md:
 * Timezone America/Recife (UTC-3) com offset explícito.
 */
private fun getIsoFormat(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("America/Recife")
    }
}

/**
 * Mapeia [TipoDto] (API) para [Tipo] (Domínio)
 */
fun TipoDto.toDomain(): Tipo {
    val imageBytes = try {
        imagem?.let { Base64.decode(it, Base64.DEFAULT) } ?: byteArrayOf()
    } catch (_: Exception) {
        byteArrayOf()
    }
    return Tipo(
        id = typeId,
        nome = nome,
        imagem = imageBytes
    )
}

/**
 * Mapeia [TipoResponseDto] (API) para [Tipo] (Domínio)
 */
fun TipoResponseDto.toDomain(): Tipo {
    val imageBytes = try {
        Base64.decode(imagem, Base64.DEFAULT)
    } catch (_: Exception) {
        byteArrayOf()
    }
    return Tipo(
        id = id,
        nome = nome,
        imagem = imageBytes
    )
}

/**
 * Mapeia [ProdutoResponseDto] (API) para [Produto] (Domínio)
 */
fun ProdutoResponseDto.toDomain(): Produto {
    val date = try {
        getIsoFormat().parse(dataValidade.replace("Z", "+0000")) ?: Date()
    } catch (_: Exception) {
        Date()
    }
    return Produto(
        id = id,
        tipo = type.toDomain(),
        marca = marca,
        dataValidade = date,
        unidade = unidade,
        unidadeMedida = unidadeMedida,
        quantidade = quantidade
    )
}

/**
 * Mapeia [Produto] (Domínio) para [ProdutoRequestDto] (API)
 */
fun Produto.toRequestDto(): ProdutoRequestDto {
    return ProdutoRequestDto(
        typeId = tipo.id,
        marca = marca,
        dataValidade = getIsoFormat().format(dataValidade),
        unidade = unidade,
        unidadeMedida = unidadeMedida,
        quantidade = quantidade
    )
}

/**
 * Mapeia [Produto] (Domínio) para [ProdutoUpdateDto] (API)
 */
fun Produto.toUpdateDto(): ProdutoUpdateDto {
    return ProdutoUpdateDto(
        typeId = tipo.id,
        marca = marca,
        dataValidade = getIsoFormat().format(dataValidade),
        unidade = unidade,
        unidadeMedida = unidadeMedida,
        quantidade = quantidade
    )
}

// --- MAPEADORES ROOM (Para compatibilidade com OfflineRepository e Testes) ---

/**
 * Mapeia [TipoEntity] (Banco de Dados) para [Tipo] (Domínio)
 */
fun TipoEntity.toDomain(): Tipo {
    return Tipo(
        id = id,
        nome = nome,
        imagem = imagem
    )
}

/**
 * Mapeia [Tipo] (Domínio) para [TipoEntity] (Banco de Dados)
 */
fun Tipo.toEntity(): TipoEntity {
    return TipoEntity(
        id = id,
        nome = nome,
        imagem = imagem
    )
}

/**
 * Mapeia [ProdutoEntity] (Banco de Dados) e [TipoEntity] (Banco de Dados) para [Produto] (Domínio)
 */
fun ProdutoEntity.toDomain(tipoEntity: TipoEntity): Produto {
    return Produto(
        id = id,
        tipo = tipoEntity.toDomain(),
        marca = marca,
        dataValidade = Date(dataValidade),
        unidade = unidade,
        unidadeMedida = unidadeMedida,
        quantidade = quantidade
    )
}

/**
 * Mapeia [Produto] (Domínio) para [ProdutoEntity] (Banco de Dados)
 */
fun Produto.toEntity(): ProdutoEntity {
    return ProdutoEntity(
        id = id,
        tipo = tipo.id,
        marca = marca,
        dataValidade = dataValidade.time,
        unidade = unidade,
        unidadeMedida = unidadeMedida,
        quantidade = quantidade
    )
}
