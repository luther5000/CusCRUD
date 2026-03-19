package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor para remover um produto do inventário.
 */
class RemoveProdutoInteractor @Inject constructor(
    private val repository: ProdutoRepository
) {
    suspend operator fun invoke(produto: Produto): Result<Unit> {
        return repository.removeProduto(produto.id)
    }
}