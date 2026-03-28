package com.cuscrud.data.local

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Suite de testes unitários para o [SessionManager].
 *
 * Valida o armazenamento e recuperação segura de tokens de autenticação
 * e identificadores de inventário ativo utilizando EncryptedSharedPreferences.
 */
class SessionManagerTest {

    private lateinit var sessionManager: SessionManager
    private val sharedPreferences: SharedPreferences = mockk(relaxed = true)
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)

    @Before
    fun setup() {
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor

        sessionManager = SessionManager(sharedPreferences)
    }

    // region Token Management Tests

    /**
     * Objetivo: Validar se o token é salvo corretamente no SharedPreferences.
     * Entradas: String de token "test_token".
     * Comportamento esperado: O método edit().putString() deve ser chamado com a chave correta e apply() deve ser invocado.
     */
    @Test
    fun `saveAuthToken should call putString and apply`() {
        val token = "test_token"

        sessionManager.saveAuthToken(token)

        verify { editor.putString("auth_token", token) }
        verify { editor.apply() }
    }

    /**
     * Objetivo: Validar a recuperação do token quando ele existe.
     * Entradas: SharedPreferences contendo "stored_token".
     * Comportamento esperado: Deve retornar "stored_token".
     */
    @Test
    fun `fetchAuthToken should return token when it exists`() {
        val expectedToken = "stored_token"
        every { sharedPreferences.getString("auth_token", null) } returns expectedToken

        val actualToken = sessionManager.fetchAuthToken()

        assertEquals(expectedToken, actualToken)
    }

    /**
     * Objetivo: Validar a limpeza do token da sessão.
     * Entradas: Chamada para clearAuthToken.
     * Comportamento esperado: O método remove() deve ser chamado com a chave do token e apply() deve ser invocado.
     */
    @Test
    fun `clearAuthToken should call remove and apply`() {
        sessionManager.clearAuthToken()

        verify { editor.remove("auth_token") }
        verify { editor.apply() }
    }

    // endregion

    // region Inventory Management Tests

    /**
     * Objetivo: Validar se o ID do inventário ativo é salvo corretamente.
     * Entradas: UUID de teste "123-abc".
     * Comportamento esperado: O método edit().putString() deve ser chamado com a chave "active_inventory_id" e apply() deve ser invocado.
     */
    @Test
    fun `saveActiveInventoryId should call putString and apply`() {
        val invId = "123-abc"

        sessionManager.saveActiveInventoryId(invId)

        verify { editor.putString("active_inventory_id", invId) }
        verify { editor.apply() }
    }

    /**
     * Objetivo: Validar a recuperação do ID do inventário ativo quando ele existe.
     * Entradas: SharedPreferences contendo "123-abc" na chave de inventário.
     * Comportamento esperado: Deve retornar "123-abc".
     */
    @Test
    fun `fetchActiveInventoryId should return ID when it exists`() {
        val expectedId = "123-abc"
        every { sharedPreferences.getString("active_inventory_id", null) } returns expectedId

        val actualId = sessionManager.fetchActiveInventoryId()

        assertEquals(expectedId, actualId)
    }

    /**
     * Objetivo: Validar a limpeza do ID do inventário ativo.
     * Entradas: Chamada para clearActiveInventoryId.
     * Comportamento esperado: O método remove() deve ser chamado com a chave "active_inventory_id" e apply() deve ser invocado.
     */
    @Test
    fun `clearActiveInventoryId should call remove and apply`() {
        sessionManager.clearActiveInventoryId()

        verify { editor.remove("active_inventory_id") }
        verify { editor.apply() }
    }

    // endregion
}
