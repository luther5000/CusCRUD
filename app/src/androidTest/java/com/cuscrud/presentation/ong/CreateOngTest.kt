package com.cuscrud.presentation.ong

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import com.cuscrud.MainActivity
import com.cuscrud.domain.repository.AuthRepository
import com.cuscrud.domain.repository.InventoryRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class CreateOngTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var inventoryRepository: InventoryRepository

    @Before
    fun setup() {
        hiltRule.inject()
        
        runBlocking {
            authRepository.logout()
            inventoryRepository.clearActiveInventory()
        }

        // Login para chegar na tela de seleção
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("joao.novo@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senhaforte456")
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Aguarda carregar a tela de seleção.
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Selecione sua ONG", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Clica no botão "Nova ONG". 
        // Usamos useUnmergedTree = true para encontrar o texto dentro do FAB
        composeTestRule.onNodeWithText("Nova ONG", ignoreCase = true, useUnmergedTree = true).performClick()
        
        // Aguarda a transição para a tela de criação
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Nome da ONG", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun criarNovaOng_ComSucesso() {
        // Quando o usuário insere um nome válido
        composeTestRule.onNodeWithText("Nome da ONG").performTextInput("ONG de Teste")
        
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("CRIAR ONG").performClick()

        // Então o sistema cria a ONG e navega para o inventário dela
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Inventário Geral", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Inventário Geral", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun criarOngNomeEmBranco_InformaErro() {
        // Quando clica em criar sem preencher o nome
        composeTestRule.onNodeWithText("CRIAR ONG").performClick()

        // Então exibe a mensagem de obrigatoriedade
        composeTestRule.onNodeWithText("O preenchimento do nome é obrigatório.").assertIsDisplayed()
    }

    @Test
    fun criarOngNomeMuitoLongo_InformaErro() {
        // Quando insere um nome com 256 caracteres
        val nomeLongo = "a".repeat(256)
        composeTestRule.onNodeWithText("Nome da ONG").performTextInput(nomeLongo)
        
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("CRIAR ONG").performClick()

        // Então exibe a mensagem de limite de caracteres
        composeTestRule.onNodeWithText("O nome da ONG deve ter no máximo 255 caracteres.").assertIsDisplayed()
    }

    /*@Test
    fun falhaConexaoAoCriarOng_InformaErroPadronizado() {
        // Quando ocorre uma falha de conexão (Simulando com servidor offline)
        composeTestRule.onNodeWithText("Nome da ONG").performTextInput("ONG Erro Rede")
        
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("CRIAR ONG").performClick()

        // Então exibe a mensagem de falha de conexão padronizada
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Não foi possível se conectar ao servidor", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }*/
}
