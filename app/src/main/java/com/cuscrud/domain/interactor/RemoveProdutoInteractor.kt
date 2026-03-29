package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor para remover um produto do inventário.
 * Refatorado para retornar [Result].
 */
class RemoveProdutoInteractor @Inject constructor(
    private val repository: ProdutoRepository
) {
    suspend operator fun invoke(produto: Int): Result<Unit> {
        return repository.removeProduto(produto)
    }
}
