package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Interactor para visualizar o inventário geral agrupado por tipo.
 * Atende ao Cenário 1 e 4 do Gherkin.
 */
class GetInventarioAgrupadoInteractor @Inject constructor(
    private val repository: ProdutoRepository
) {
    operator fun invoke(): Flow<Result<Map<Tipo, List<Produto>>>> {
        return repository.getAllProdutos()
            .map { produtos ->
                val agrupado = produtos.groupBy { it.tipo }
                Result.Success(agrupado) as Result<Map<Tipo, List<Produto>>>
            }
            .catch { 
                // Mensagem de erro conforme Cenário 4: "não foi possível carregá-lo"
                emit(Result.Error(Exception("não foi possível carregá-lo"))) 
            }
    }
}