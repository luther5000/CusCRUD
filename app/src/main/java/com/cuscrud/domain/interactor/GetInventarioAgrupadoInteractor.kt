package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor para visualizar o inventário geral agrupado por tipo.
 * Refatorado para retornar [Result] (One-shot).
 */
class GetInventarioAgrupadoInteractor @Inject constructor(
    private val repository: ProdutoRepository
) {
    suspend operator fun invoke(): Result<Map<Tipo, List<Produto>>> {
        return when (val result = repository.getProdutos()) {
            is Result.Success -> {
                val agrupado = result.data.groupBy { it.tipo }
                Result.Success(agrupado)
            }
            is Result.Error -> result
            Result.Loading -> Result.Loading
        }
    }
}
