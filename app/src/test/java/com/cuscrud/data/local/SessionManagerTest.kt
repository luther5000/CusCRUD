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
 * o Jetpack DataStore. Garante que tokens de autenticação, credenciais de usuário para 
 * Silent Login e o contexto do inventário ativo (ID e Nível de Acesso) sejam armazenados 
 * e recuperados de forma íntegra entre sessões.
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

    // region Bloco: Gestão de Token de Autenticação e Credenciais

    /**
     * Objetivo: Persistir o token JWT após o login.
     * Entradas: Uma string de token válida ("test_token").
     * Critério de Aceitação: O valor recuperado via fetchAuthToken deve ser idêntico ao valor salvo.
     */
    @Test
    fun `saveAuthToken should store token correctly`() = runTest {
        val token = "test_token"
        
        sessionManager.saveAuthToken(token)
        
        val storedToken = sessionManager.fetchAuthToken()
        assertEquals(token, storedToken)
    }

    /**
     * Objetivo: Garantir que o login e senha sejam salvos de forma segura para Silent Login.
     * Entradas: Login "user@test.com" e senha "password123".
     * Critério de Aceitação: fetchCredentials() deve retornar o Pair contendo exatamente os valores salvos.
     */
    @Test
    fun `saveCredentials should store login and password correctly`() = runTest {
        val login = "user@test.com"
        val pass = "password123"

        sessionManager.saveCredentials(login, pass)

        val stored = sessionManager.fetchCredentials()
        assertEquals(login, stored?.first)
        assertEquals(pass, stored?.second)
    }

    /**
     * Objetivo: Validar a recuperação de credenciais quando não há dados salvos.
     * Entradas: DataStore vazio.
     * Critério de Aceitação: fetchCredentials() deve retornar null.
     */
    @Test
    fun `fetchCredentials should return null when no credentials stored`() = runTest {
        val stored = sessionManager.fetchCredentials()
        assertNull(stored)
    }

    /**
     * Objetivo: Remover as credenciais e o token do dispositivo durante o logout.
     * Entradas: Token e credenciais previamente salvos.
     * Critério de Aceitação: Após clearAuthToken(), a recuperação do token e das credenciais deve retornar null.
     */
    @Test
    fun `clearAuthToken should remove stored token and credentials`() = runTest {
        sessionManager.saveAuthToken("token_to_clear")
        sessionManager.saveCredentials("user", "pass")
        
        sessionManager.clearAuthToken()
        
        assertNull(sessionManager.fetchAuthToken())
        assertNull(sessionManager.fetchCredentials())
    }

    /**
     * Objetivo: Limpar especificamente as credenciais de login.
     * Entradas: Credenciais salvas.
     * Critério de Aceitação: fetchCredentials() retorna null após a limpeza.
     */
    @Test
    fun `clearCredentials should remove stored login and password`() = runTest {
        sessionManager.saveCredentials("user", "pass")
        
        sessionManager.clearCredentials()
        
        assertNull(sessionManager.fetchCredentials())
    }

    // endregion

    // region Bloco: Contexto de Inventário Ativo

    /**
     * Objetivo: Armazenar qual inventário o usuário está visualizando no momento.
     * Entradas: UUID de um inventário ("123-abc").
     * Critério de Aceitação: O ID recuperado deve ser persistente e idêntico ao salvo.
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
     * Critério de Aceitação: Retornar null ao buscar o ID ativo após a limpeza.
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
     * Critério de Aceitação: O valor recuperado deve refletir a permissão correta (-1 se não existir).
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
