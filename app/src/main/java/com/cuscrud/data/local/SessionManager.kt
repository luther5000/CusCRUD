package com.cuscrud.data.local

import android.content.SharedPreferences
import com.cuscrud.di.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerencia a sessão do usuário utilizando armazenamento seguro para o token JWT
 * e o ID do inventário ativo.
 *
 * @param sharedPreferences Instância de SharedPreferences (preferencialmente criptografada) injetada.
 */
@Singleton
class SessionManager @Inject constructor(
    @SecureStorage private val sharedPreferences: SharedPreferences
) {

    /**
     * Salva o token de autenticação de forma segura.
     * @param token O token JWT retornado pela API.
     */
    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Recupera o token de autenticação salvo.
     * @return O token JWT ou null se não existir.
     */
    fun fetchAuthToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    /**
     * Remove o token de autenticação e encerra a sessão local.
     */
    fun clearAuthToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).apply()
    }

    /**
     * Salva o ID do inventário ativo.
     * @param invId UUID do inventário.
     */
    fun saveActiveInventoryId(invId: String) {
        sharedPreferences.edit().putString(KEY_ACTIVE_INV_ID, invId).apply()
    }

    /**
     * Recupera o ID do inventário ativo salvo.
     * @return O UUID ou null se não houver inventário selecionado.
     */
    fun fetchActiveInventoryId(): String? {
        return sharedPreferences.getString(KEY_ACTIVE_INV_ID, null)
    }

    /**
     * Limpa o ID do inventário ativo.
     */
    fun clearActiveInventoryId() {
        sharedPreferences.edit().remove(KEY_ACTIVE_INV_ID).apply()
    }

    /**
     * Salva a role associada ao inventário ativo.
     * @param role O valor inteiro da Role.
     */
    fun saveActiveInventoryRole(role: Int) {
        sharedPreferences.edit().putInt(KEY_ACTIVE_INV_ROLE, role).apply()
    }

    /**
     * Recupera a role associada ao inventário ativo.
     * @return O valor inteiro ou -1 se não houver.
     */
    fun fetchActiveInventoryRole(): Int {
        return sharedPreferences.getInt(KEY_ACTIVE_INV_ROLE, -1)
    }

    /**
     * Limpa a role do inventário ativo.
     */
    fun clearActiveInventoryRole() {
        sharedPreferences.edit().remove(KEY_ACTIVE_INV_ROLE).apply()
    }

    companion object {
        const val KEY_TOKEN = "auth_token"
        const val KEY_ACTIVE_INV_ID = "active_inventory_id"
        const val KEY_ACTIVE_INV_ROLE = "active_inventory_role"
    }
}
