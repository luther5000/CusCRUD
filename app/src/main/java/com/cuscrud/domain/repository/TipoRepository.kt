package com.cuscrud.domain.repository

import com.cuscrud.domain.model.Tipo
import kotlinx.coroutines.flow.Flow

/**
 * Interface de repositório para operações relacionadas a [Tipo].
 * Mantida para compatibilidade com a UI atual e o fluxo reativo local.
 */
interface TipoRepository {
    /**
     * Retorna todos os tipos como um [Flow] de uma [List] de [Tipo].
     * Esse fluxo emite novos valores sempre que os dados subjacentes mudam.
     */
    fun getAllTipos(): Flow<List<Tipo>>

    /**
     * Insere um novo [Tipo] no repositório local.
     */
    suspend fun insertTipo(tipo: Tipo)

    /**
     * Remove um tipo por meio do seu [id].
     * @return O [Tipo] removido ou null se nenhum tipo foi encontrado.
     */
    suspend fun removeTipo(id: Long): Tipo?

    /**
     * Atualiza um tipo existente identificado por um [id] com os dados do [tipo].
     * @return O [Tipo] atualizado ou null se não houver tipo com o id especificado.
     */
    suspend fun editTipo(id: Long, tipo: Tipo): Tipo?
}
