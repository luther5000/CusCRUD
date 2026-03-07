package com.cuscrud.domain.repository

import com.cuscrud.domain.model.Tipo
import kotlinx.coroutines.flow.Flow

/**
 * Interface de repositório para operações relacionadas a [Tipo].
 *
 * ### Visão Geral de Coroutines e Flow:
 * Este repositório utiliza Kotlin Coroutines e Flow para gerenciar operações de dados assíncronas de forma eficiente.
 *
 * **Coroutines (funções `suspend`):**
 * - Usadas para operações únicas ("one-shot") como [insertTipo], [removeTipo] ou [editTipo].
 * - A palavra-chave `suspend` indica que a função pode pausar a sua execução sem bloquear a thread principal,
 *   esperando a conclusão de uma operação de banco de dados ou rede e, em seguida, retomando de onde parou.
 * - Isso evita que a interface do usuário (UI) trave ou apresente lentidão durante operações pesadas.
 *
 * **Flow (`Flow<T>`):**
 * - Usado para operações de fluxo ("stream") como [getAllTipos].
 * - Um `Flow` é um fluxo reativo que pode emitir múltiplos valores ao longo do tempo (como uma torneira aberta).
 * - Neste projeto, o banco de dados Room usa Flow para notificar automaticamente a UI sempre que os dados mudam.
 *   Se você inserir uma nova categoria, [getAllTipos] emitirá automaticamente uma lista nova e atualizada para todos os seus observadores.
 *
 * ### Operadores Úteis de Flow:
 * - `map`: Transforma cada lista emitida (ex: para ordenar os itens ou converter tipos).
 * - `filter`: Filtra as emissões para que a UI receba apenas o que é relevante.
 * - `combine`: Permite unir este fluxo com outro (ex: unir Tipos e Produtos) para gerar um novo resultado.
 * - `stateIn`: Converte o fluxo em um `StateFlow` dentro do ViewModel, garantindo que a UI tenha sempre o dado mais recente de forma segura.
 * - `collect`: O comando que efetivamente "abre a torneira" e começa a receber os dados para processá-los.
 */
interface TipoRepository {
    /**
     * Retorna todos os tipos como um [Flow] de uma [List] de [Tipo].
     * Esse fluxo emite novos valores sempre que os dados subjacentes mudam.
     */
    fun getAllTipos(): Flow<List<Tipo>>

    /**
     * Insere um novo [Tipo] no repositório.
     */
    suspend fun insertTipo(tipo: Tipo)

    /**
     * Remove um tipo por meio do seu [id].
     * @return O [Tipo] removido ou null se nenhum tipo foi encontrado com aquele [id].
     */
    suspend fun removeTipo(id: Long): Tipo?

    /**
     * Atualiza um tipo existente identificado por um [id] com os dados do [tipo].
     * @return O [Tipo] atualizado ou null se não houver tipo com o id especificado.
     */
    suspend fun editTipo(id: Long, tipo: Tipo): Tipo?
}
