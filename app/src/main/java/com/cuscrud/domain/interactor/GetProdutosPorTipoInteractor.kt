package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Interactor para visualizar os produtos de um tipo específico.
 * Atende ao Cenário 2 e 5 do Gherkin.
 */
class GetProdutosPorTipoInteractor @Inject constructor(
    private val repository: ProdutoRepository
) {
    operator fun invoke(tipoId: Long): Flow<Result<List<Produto>>> {
        return repository.getProdutosByTipo(tipoId)
            .map { produtos ->
                Result.Success(produtos) as Result<List<Produto>>
            }
            .catch {
                // Mensagem de erro conforme Cenário 5: "não foi possível carregá-los"
                emit(Result.Error(Exception("não foi possível carregá-los")))
            }
    }
}