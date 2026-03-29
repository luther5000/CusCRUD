package com.cuscrud.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Suite de testes unitários para o [SessionManager].
 * 
 * Esta classe valida a persistência de dados sensíveis e de estado da aplicação utilizando 
 * o Jetpack DataStore. Garante que tokens de autenticação e o contexto do inventário ativo 
 * (ID e Nível de Acesso) sejam armazenados e recuperados de forma íntegra entre sessões.
 * 
 * Utiliza uma instância real do DataStore apontando para um arquivo temporário para
 * máxima fidelidade ao comportamento em runtime.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // Cria um DataStore real isolado para o ambiente de teste
    private val dataStore = PreferenceDataStoreFactory.create(
        scope = testScope,
        produceFile = { tmpFolder.newFile("test.preferences_pb") }
    )

    private val sessionManager = SessionManager(dataStore)

    // region Bloco: Gestão de Token de Autenticação

    /**
     * Objetivo: Persistir o token JWT após o login.
     * Entradas: Uma string de token válida.
     * Critério de Aceitação: O valor recuperado deve ser idêntico ao valor salvo.
     */
    @Test
    fun `saveAuthToken should store token correctly`() = runTest {
        val token = "test_token"
        
        sessionManager.saveAuthToken(token)
        
        val storedToken = sessionManager.fetchAuthToken()
        assertEquals(token, storedToken)
    }

    /**
     * Objetivo: Remover as credenciais do dispositivo durante o logout.
     * Entradas: Token previamente salvo.
     * Critério de Aceitação: Após a limpeza, a recuperação do token deve retornar null.
     */
    @Test
    fun `clearAuthToken should remove stored token`() = runTest {
        sessionManager.saveAuthToken("token_to_clear")
        
        sessionManager.clearAuthToken()
        
        val storedToken = sessionManager.fetchAuthToken()
        assertNull(storedToken)
    }

    // endregion

    // region Bloco: Contexto de Inventário Ativo

    /**
     * Objetivo: Armazenar qual inventário o usuário está visualizando no momento.
     * Entradas: UUID de um inventário.
     * Critério de Aceitação: O ID recuperado deve ser persistente.
     */
    @Test
    fun `saveActiveInventoryId should store ID correctly`() = runTest {
        val invId = "123-abc"
        
        sessionManager.saveActiveInventoryId(invId)
        
        val storedId = sessionManager.fetchActiveInventoryId()
        assertEquals(invId, storedId)
    }

    /**
     * Objetivo: Limpar a seleção de inventário.
     * Entradas: ID de inventário previamente selecionado.
     * Critério de Aceitação: Retornar null ao buscar o ID ativo.
     */
    @Test
    fun `clearActiveInventoryId should remove stored ID`() = runTest {
        sessionManager.saveActiveInventoryId("id-to-clear")
        
        sessionManager.clearActiveInventoryId()
        
        val storedId = sessionManager.fetchActiveInventoryId()
        assertNull(storedId)
    }

    /**
     * Objetivo: Persistir o nível de permissão (Role) do usuário no inventário atual.
     * Entradas: Inteiro representando a Role (ex: 1 para EDITOR).
     * Critério de Aceitação: O valor recuperado deve refletir a permissão correta para controle de UI.
     */
    @Test
    fun `saveActiveInventoryRole should store role correctly`() = runTest {
        val role = 1 // Ex: Editor
        
        sessionManager.saveActiveInventoryRole(role)
        
        val storedRole = sessionManager.fetchActiveInventoryRole()
        assertEquals(role, storedRole)
    }

    // endregion
}
