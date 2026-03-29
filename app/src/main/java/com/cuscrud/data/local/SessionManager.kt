package com.cuscrud.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cuscrud.di.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerencia o estado persistente da sessão do usuário e preferências de contexto da aplicação.
 *
 * Esta classe utiliza o Jetpack DataStore para armazenar de forma segura:
 * - **Token de Autenticação (JWT)**: Mantém o usuário logado entre as sessões do app.
 * - **ID do Inventário Ativo**: Identifica qual inventário o usuário está visualizando/editando atualmente.
 * - **Papel (Role) do Usuário**: Armazena o nível de acesso do usuário no inventário selecionado para controle de UI.
 *
 * O uso do qualificador `@SecureStorage` indica que os dados são persistidos em uma instância do DataStore
 * configurada para armazenamento seguro. Fornece dados de forma reativa através de [Flow] e de forma pontual
 * via funções suspensas (suspend).
 */

@Singleton
class SessionManager @Inject constructor(
    @field:SecureStorage private val dataStore: DataStore<Preferences>
) {

    private val keyToken = stringPreferencesKey("auth_token")
    private val keyActiveInvId = stringPreferencesKey("active_inventory_id")
    private val keyActiveInvRole = intPreferencesKey("active_inventory_role")

    /**
     * Salva o token de autenticação.
     */
    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[keyToken] = token
        }
    }

    /**
     * Recupera o token de autenticação como um Flow.
     */
    val authTokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[keyToken]
    }

    /**
     * Recupera o token de forma síncrona (suspend).
     */
    suspend fun fetchAuthToken(): String? = authTokenFlow.first()

    /**
     * Remove o token de autenticação.
     */
    suspend fun clearAuthToken() {
        dataStore.edit { preferences ->
            preferences.remove(keyToken)
        }
    }

    /**
     * Salva o ID do inventário ativo.
     */
    suspend fun saveActiveInventoryId(invId: String) {
        dataStore.edit { preferences ->
            preferences[keyActiveInvId] = invId
        }
    }

    /**
     * Recupera o ID do inventário ativo.
     */
    val activeInventoryIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[keyActiveInvId]
    }

    suspend fun fetchActiveInventoryId(): String? = activeInventoryIdFlow.first()

    /**
     * Limpa o ID do inventário ativo.
     */
    suspend fun clearActiveInventoryId() {
        dataStore.edit { preferences ->
            preferences.remove(keyActiveInvId)
        }
    }

    /**
     * Salva a role associada ao inventário ativo.
     */
    suspend fun saveActiveInventoryRole(role: Int) {
        dataStore.edit { preferences ->
            preferences[keyActiveInvRole] = role
        }
    }

    /**
     * Recupera a role do inventário ativo.
     */
    val activeInventoryRoleFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[keyActiveInvRole] ?: -1
    }

    suspend fun fetchActiveInventoryRole(): Int = activeInventoryRoleFlow.first()

    /**
     * Limpa a role do inventário ativo.
     */
    suspend fun clearActiveInventoryRole() {
        dataStore.edit { preferences ->
            preferences.remove(keyActiveInvRole)
        }
    }
}
