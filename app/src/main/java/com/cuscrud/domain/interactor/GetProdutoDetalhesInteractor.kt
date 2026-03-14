package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Interactor para visualizar os detalhes de um produto específico.
 * Atende ao Cenário 3 e 6 do Gherkin.
 */
class GetProdutoDetalhesInteractor @Inject constructor(
    private val repository: ProdutoRepository
) {
    operator fun invoke(produtoId: Int): Flow<Result<Produto?>> = flow {
        emit(Result.Loading)
        try {
            val produto = repository.getProdutoById(produtoId)
            emit(Result.Success(produto))
        } catch (e: Exception) {
            // Mensagem de erro conforme Cenário 6: "não foi possível carregá-lo"
            emit(Result.Error(Exception("não foi possível carregá-lo")))
        }
    }
}