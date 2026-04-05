package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.repository.TipoRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor para visualizar o inventário geral agrupado por tipo.
 * Realiza o "join" entre produtos e categorias para garantir que os nomes apareçam na UI.
 */
class GetInventarioAgrupadoInteractor @Inject constructor(
    private val produtoRepository: ProdutoRepository,
    private val tipoRepository: TipoRepository
) {
    suspend operator fun invoke(): Result<Map<Tipo, List<Produto>>> {
        // 1. Busca os produtos
        val produtosResult = produtoRepository.getProdutos()
        if (produtosResult is Result.Error) return Result.Error(produtosResult.exception)
        if (produtosResult is Result.Loading) return Result.Loading
        
        val produtosRaw = (produtosResult as Result.Success).data

        // 2. Busca os tipos para obter os nomes
        val tiposResult = tipoRepository.getTipos()
        val tiposMap = if (tiposResult is Result.Success) {
            tiposResult.data.associateBy { it.id }
        } else {
            emptyMap()
        }

        // 3. Enriquece os produtos com os nomes das categorias e agrupa
        val agrupado = produtosRaw.map { produto ->
            val tipoEnriquecido = tiposMap[produto.tipo.id] ?: produto.tipo
            produto.copy(tipo = tipoEnriquecido)
        }.groupBy { it.tipo }

        return Result.Success(agrupado)
    }
}
