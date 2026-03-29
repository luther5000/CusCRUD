package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor para visualizar os detalhes de um produto específico.
 * Refatorado para lidar com o retorno [Result] do repositório (One-shot).
 */
class GetProdutoDetalhesInteractor @Inject constructor(
    private val repository: ProdutoRepository
) {
    suspend operator fun invoke(produtoId: Int): Result<Produto> {
        return repository.getProdutoById(produtoId)
    }
}
