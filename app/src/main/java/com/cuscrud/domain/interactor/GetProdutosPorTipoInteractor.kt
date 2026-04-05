package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.repository.TipoRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor para visualizar os produtos de um tipo específico.
 * Realiza o enriquecimento dos dados do Tipo para garantir que o nome apareça na UI.
 */
class GetProdutosPorTipoInteractor @Inject constructor(
    private val produtoRepository: ProdutoRepository,
    private val tipoRepository: TipoRepository
) {
    suspend operator fun invoke(tipoId: Long): Result<List<Produto>> {
        val produtosResult = produtoRepository.getProdutosByTipo(tipoId)
        
        if (produtosResult is Result.Success) {
            val tipoResult = tipoRepository.getTipoById(tipoId)
            val tipoEnriquecido = if (tipoResult is Result.Success) tipoResult.data else null
            
            val produtosEnriquecidos = produtosResult.data.map { produto ->
                if (tipoEnriquecido != null) {
                    produto.copy(tipo = tipoEnriquecido)
                } else {
                    produto
                }
            }
            return Result.Success(produtosEnriquecidos)
        }
        
        return produtosResult
    }
}
