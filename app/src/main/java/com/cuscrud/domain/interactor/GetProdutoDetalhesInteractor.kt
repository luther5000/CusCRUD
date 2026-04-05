package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.repository.TipoRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor para visualizar os detalhes de um produto específico.
 * Realiza o enriquecimento dos dados do Tipo para garantir que o nome apareça na UI.
 */
class GetProdutoDetalhesInteractor @Inject constructor(
    private val produtoRepository: ProdutoRepository,
    private val tipoRepository: TipoRepository
) {
    suspend operator fun invoke(produtoId: Long): Result<Produto> {
        val produtoResult = produtoRepository.getProdutoById(produtoId)
        
        if (produtoResult is Result.Success) {
            val produto = produtoResult.data
            // Busca o nome do tipo para enriquecer o objeto de domínio
            val tipoResult = tipoRepository.getTipoById(produto.tipo.id)
            return if (tipoResult is Result.Success) {
                Result.Success(produto.copy(tipo = tipoResult.data))
            } else {
                Result.Success(produto) // Fallback para o produto original se o tipo falhar
            }
        }
        
        return produtoResult
    }
}
