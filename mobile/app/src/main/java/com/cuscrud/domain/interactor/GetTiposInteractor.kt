package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.TipoRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor para obter a lista de todos os tipos de produtos.
 * Refatorado para chamadas assíncronas via API REST.
 */
class GetTiposInteractor @Inject constructor(
    private val repository: TipoRepository
) {
    /**
     * Executa a busca de tipos.
     * @param limit Limite de itens.
     * @param offset Deslocamento.
     * @return [Result] com a lista de tipos.
     */
    suspend operator fun invoke(limit: Int = 100, offset: Int = 0): Result<List<Tipo>> {
        return try {
            repository.getTipos(limit, offset)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
