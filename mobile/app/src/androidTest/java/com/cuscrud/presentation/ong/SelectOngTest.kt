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
import kotlin.collections.isNotEmpty

@HiltAndroidTest
class SelectOngTest {

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
        
        // Garante que o teste comece na tela de login limpando a sessão
        runBlocking {
            authRepository.logout()
            inventoryRepository.clearActiveInventory()
        }

        // Realiza login para chegar na tela de seleção de ONG
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("joao.novo@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senhaforte456")
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Aguarda carregar a tela de seleção de ONG
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Selecione sua ONG", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun alternarEntreOngs_ComSucesso() {
        // Dado que estou na lista de ONGs e vejo a "ONG B"
        composeTestRule.onNodeWithText("ONG B", ignoreCase = true, substring = true).assertIsDisplayed()

        // Quando seleciono a "ONG B"
        composeTestRule.onNodeWithText("ONG B", ignoreCase = true, substring = true).performClick()

        // Então o aplicativo deve navegar para a tela de Inventário
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Inventário", substring=true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Valida que o cabeçalho agora exibe o contexto do Inventário
        composeTestRule.onNodeWithText("Inventário", substring=true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun voltarParaSelecaoDeOng_AposSelecionarUma() {
        // 1. Seleciona uma ONG primeiro
        composeTestRule.onNodeWithText("ONG A", ignoreCase = true, substring = true).performClick()
        
        // 2. Aguarda chegar no inventário
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Inventário", substring=true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Quando o usuário clica no botão de voltar na TopAppBar
        composeTestRule.onNodeWithContentDescription("Voltar para ONGs").performClick()

        // Então o sistema deve retornar para a tela de seleção de ONG
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Selecione sua ONG", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

   /* @Test
    fun falhaConexaoAoAlternarOng_ExibeMensagemErroEPermaneceNaTela() {
        // Nota: Simular falha de rede aqui exigiria derrubar o servidor ou interceptor.
        // Como o setActive no ViewModel atual é local, o erro de conexão 
        // apareceria caso houvesse uma validação remota no momento da troca.
        
        // Se o servidor for desativado e tentarmos clicar:
        composeTestRule.onNodeWithText("ONG A", ignoreCase = true, substring = true).performClick()

        // Então o sistema deve exibir a mensagem padronizada de falha de conexão
         composeTestRule.waitUntil(10000) {
             composeTestRule.onAllNodesWithText("Não foi possível se conectar ao servidor", substring = true, ignoreCase = true)
                 .fetchSemanticsNodes().isNotEmpty()
         }

         // E permanece na tela de seleção
         composeTestRule.onNodeWithText("Selecione sua ONG", ignoreCase = true).assertIsDisplayed()

    }*/
}
