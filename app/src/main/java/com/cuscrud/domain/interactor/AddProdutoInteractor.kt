package com.cuscrud.domain.interactor

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.util.Result
import javax.inject.Inject

/**
 * Interactor para adicionar um novo produto ao inventário.
 * Refatorado para lidar com o retorno [Result] do repositório.
 */
class AddProdutoInteractor @Inject constructor(
    private val repository: ProdutoRepository
) {
    suspend operator fun invoke(produto: Produto): Result<Unit> {
        // Validação de campos obrigatórios
        if (produto.marca.isBlank() || produto.unidadeMedida.isBlank()) {
            return Result.Error(IllegalArgumentException("é necessário preencher todos os campos obrigatórios para fazer a adição"))
        }

        // Validação de unidade negativa
        if (produto.unidade < 0) {
            return Result.Error(IllegalArgumentException("unidade inválida"))
        }

        // Validação de quantidade
        if (produto.quantidade < 0) {
            return Result.Error(IllegalArgumentException("é necessário informar uma quantidade positiva para fazer a adição"))
        }

        // O repositório já retorna Result e encapsula try-catch
        return repository.insertProduto(produto)
    }
}
