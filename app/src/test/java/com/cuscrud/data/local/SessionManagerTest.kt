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
 * Utiliza uma instância real do DataStore com um arquivo temporário para garantir
 * que a lógica de persistência e recuperação de dados esteja correta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // Cria um DataStore real para os testes
    private val dataStore = PreferenceDataStoreFactory.create(
        scope = testScope,
        produceFile = { tmpFolder.newFile("test.preferences_pb") }
    )

    private val sessionManager = SessionManager(dataStore)

    @Test
    fun `saveAuthToken should store token correctly`() = runTest {
        val token = "test_token"
        
        sessionManager.saveAuthToken(token)
        
        val storedToken = sessionManager.fetchAuthToken()
        assertEquals(token, storedToken)
    }

    @Test
    fun `clearAuthToken should remove stored token`() = runTest {
        sessionManager.saveAuthToken("token_to_clear")
        
        sessionManager.clearAuthToken()
        
        val storedToken = sessionManager.fetchAuthToken()
        assertNull(storedToken)
    }

    @Test
    fun `saveActiveInventoryId should store ID correctly`() = runTest {
        val invId = "123-abc"
        
        sessionManager.saveActiveInventoryId(invId)
        
        val storedId = sessionManager.fetchActiveInventoryId()
        assertEquals(invId, storedId)
    }

    @Test
    fun `clearActiveInventoryId should remove stored ID`() = runTest {
        sessionManager.saveActiveInventoryId("id-to-clear")
        
        sessionManager.clearActiveInventoryId()
        
        val storedId = sessionManager.fetchActiveInventoryId()
        assertNull(storedId)
    }

    @Test
    fun `saveActiveInventoryRole should store role correctly`() = runTest {
        val role = 1 // Ex: Editor
        
        sessionManager.saveActiveInventoryRole(role)
        
        val storedRole = sessionManager.fetchActiveInventoryRole()
        assertEquals(role, storedRole)
    }
}
