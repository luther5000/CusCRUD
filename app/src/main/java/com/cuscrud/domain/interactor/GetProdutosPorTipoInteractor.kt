package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor para visualizar os produtos de um tipo específico.
 * Refatorado para retornar [Result] (One-shot).
 */
class GetProdutosPorTipoInteractor @Inject constructor(
    private val repository: ProdutoRepository
) {
    suspend operator fun invoke(tipoId: Long): Result<List<Produto>> {
        return repository.getProdutosByTipo(tipoId)
    }
}
