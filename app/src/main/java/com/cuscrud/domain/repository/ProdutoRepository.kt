package com.cuscrud.domain.repository

import com.cuscrud.domain.model.Produto
import kotlinx.coroutines.flow.Flow

/**
 * Repositório para operações relacionadas com [Produto].
 */
interface ProdutoRepository {
    /**
     * Retorna todos os produtos como um [Flow] de uma [List] de [Produto].
     * Esse fluxo emite novos valores sempre que os dados subjacentes mudam.
     */
    fun getAllProdutos(): Flow<List<Produto>>

    /**
     * Insere um novo [Produto] no repositório.
     */
    suspend fun insertProduto(produto: Produto)
    
    /**
     * Remove um produto por meio do seu [id].
     * @return O [Produto] removido ou null se não houver produto com o id especificado.
     */
    suspend fun removeProduto(id: Int): Produto?

    /**
     * Retorna todos os produtos que pertencem a um [tipoId] especifico
     * como um [Flow] de uma [List] de [Produto].
     * Esse fluxo emite novos valores sempre que os dados subjacentes mudam.
     */
    fun getProdutosByTipo(tipoId: Long): Flow<List<Produto>>

    /**
     * Atualiza um produto existente identificado por um [id] com os dados do [produto].
     * @return O [Produto] atualizado ou null se não houver produto com o id especificado.
     */
    suspend fun editProduto(id: Int, produto: Produto): Produto?
}
