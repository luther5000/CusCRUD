package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import javax.inject.Inject

/**
 * Interactor para remover um produto do inventário.
 */
class RemoveProdutoInteractor @Inject constructor(
    private val repository: ProdutoRepository
) {
    /**
     * Remove um produto.
     * @return O [Produto] removido ou null em caso de erro.
     */
    suspend operator fun invoke(produto: Produto): Produto? {
        return repository.removeProduto(produto.id)
    }
}