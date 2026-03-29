package com.cuscrud.domain.repository

import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.util.Result

/**
 * Interface de repositório para operações relacionadas a [Tipo].
 * Refatorada para suportar chamadas assíncronas (one-shot) via API REST conforme architecture.md.
 */
interface TipoRepository {
    /**
     * Retorna uma lista paginada de tipos.
     * @param limit Limite de itens por página.
     * @param offset Deslocamento para paginação.
     * @return [Result] contendo a lista de [Tipo] ou erro.
     */
    suspend fun getTipos(limit: Int = 20, offset: Int = 0): Result<List<Tipo>>

    /**
     * Busca um tipo específico pelo seu identificador.
     * @param id Identificador único do tipo.
     * @return [Result] contendo o [Tipo] encontrado ou erro.
     */
    suspend fun getTipoById(id: Long): Result<Tipo>

    /**
     * Insere um novo [Tipo].
     * @param nome Nome do tipo.
     * @param imagemBase64 Imagem opcional em formato Base64.
     * @return [Result] contendo o [Tipo] criado ou erro.
     */
    suspend fun insertTipo(nome: String, imagemBase64: String? = null): Result<Tipo>

    /**
     * Remove um tipo por meio do seu [id].
     * @param id Identificador do tipo a ser removido.
     * @return [Result] indicando sucesso ou erro (ex: 409 se houver produtos vinculados).
     */
    suspend fun removeTipo(id: Long): Result<Unit>

    /**
     * Atualiza um tipo existente.
     * @param id Identificador do tipo.
     * @param nome Novo nome opcional.
     * @param imagemBase64 Nova imagem opcional em formato Base64.
     * @return [Result] contendo o [Tipo] atualizado ou erro.
     */
    suspend fun editTipo(id: Long, nome: String? = null, imagemBase64: String? = null): Result<Tipo>
}
