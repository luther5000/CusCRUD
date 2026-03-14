package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.TipoRepository
import com.cuscrud.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Interactor para obter a lista de todos os tipos de produtos.
 */
class GetTiposInteractor @Inject constructor(
    private val repository: TipoRepository
) {
    operator fun invoke(): Flow<Result<List<Tipo>>> {
        return repository.getAllTipos()
            .map { tipos ->
                Result.Success(tipos) as Result<List<Tipo>>
            }
            .catch {
                emit(Result.Error(Exception("não foi possível carregar os tipos de produtos")))
            }
    }
}
