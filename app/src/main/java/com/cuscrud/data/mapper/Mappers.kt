package com.cuscrud.data.mapper

import com.cuscrud.data.local.entities.ProdutoEntity
import com.cuscrud.data.local.entities.TipoEntity
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import java.util.Date

/**
 * Maps [TipoEntity] (Database) to [Tipo] (Domain)
 */
fun TipoEntity.toDomain(): Tipo {
    return Tipo(
        id = id,
        nome = nome,
        imagem = imagem
    )
}

/**
 * Maps [Tipo] (Domain) to [TipoEntity] (Database)
 */
fun Tipo.toEntity(): TipoEntity {
    return TipoEntity(
        id = id,
        nome = nome,
        imagem = imagem
    )
}

/**
 * Maps [ProdutoEntity] (Database) and [TipoEntity] (Database) to [Produto] (Domain)
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
 * Maps [Produto] (Domain) to [ProdutoEntity] (Database)
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
