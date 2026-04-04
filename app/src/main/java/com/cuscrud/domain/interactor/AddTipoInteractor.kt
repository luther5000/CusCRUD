package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.TipoRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor responsável por adicionar um novo tipo (categoria) de produto.
 */
class AddTipoInteractor @Inject constructor(
    private val repository: TipoRepository
) {
    suspend operator fun invoke(nome: String, imagemBase64: String? = null): Result<Tipo> {
        if (nome.isBlank()) {
            return Result.Error(Exception("O nome da categoria não pode estar em branco."))
        }
        return repository.insertTipo(nome, imagemBase64)
    }
}
