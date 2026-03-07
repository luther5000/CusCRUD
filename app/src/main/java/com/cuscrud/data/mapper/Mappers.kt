package com.cuscrud.data.mapper

import com.cuscrud.data.local.entities.ProdutoEntity
import com.cuscrud.data.local.entities.TipoEntity
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import java.util.Date

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
