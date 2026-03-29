package com.cuscrud.domain.repository

import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.util.Result

/**
 * Repositório para operações relacionadas com [Produto].
 * Refatorado para operações one-shot via API REST retornando [Result].
 */

interface ProdutoRepository {
    /**
     * Retorna uma lista de produtos vinculados ao inventário ativo.
     * @param limit Limite de itens (paginação).
     * @param offset Deslocamento (paginação).
     */
    suspend fun getProdutos(limit: Int = 100, offset: Int = 0): Result<List<Produto>>

    /**
     * Insere um novo [Produto] no repositório.
     */
    suspend fun insertProduto(produto: Produto): Result<Unit>
    
    /**
     * Remove um produto por meio do seu [id].
     */
    suspend fun removeProduto(id: Int): Result<Unit>

    /**
     * Retorna todos os produtos que pertencem a um [tipoId] especifico.
     */
    suspend fun getProdutosByTipo(tipoId: Long): Result<List<Produto>>

    /**
     * Atualiza um produto existente identificado por um [id] com os dados do [produto].
     */
    suspend fun editProduto(id: Int, produto: Produto): Result<Produto>

    /**
     * Retorna um produto especifico pelo seu [id].
     */
    suspend fun getProdutoById(id: Int): Result<Produto>
}
