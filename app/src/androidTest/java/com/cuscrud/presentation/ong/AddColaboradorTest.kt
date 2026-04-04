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
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters

@HiltAndroidTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // <--- OBRIGA A ORDEM ALFABÉTICA
class AddColaboradorTest {

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

        // 1. Login (Dono da ONG)
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("joao.novo@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senhaforte456")
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // 2. Seleciona uma ONG
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Selecione sua ONG", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("ONG A", substring = true).performClick()

        // 3. Vai para configurações
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Inventário Geral", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Configurações da ONG").performClick()

        // 4. Clica em Adicionar Colaborador
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Adicionar").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Adicionar").performClick()
    }

    @Test
    fun teste01_adicionarColaboradorEditor_ComSucesso() {
        composeTestRule.onNodeWithText("E-mail do usuário").performTextInput("colaborador@teste.com")
        composeTestRule.onNodeWithText("Editor").performClick()

        Espresso.closeSoftKeyboard()
        composeTestRule.onAllNodesWithText("Adicionar", useUnmergedTree = true).onLast().performClick()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Colaborador adicionado com sucesso!", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun teste02_adicionarColaboradorVisualizador_ComSucesso() {
        composeTestRule.onNodeWithText("E-mail do usuário").performTextInput("visualizador@teste.com")
        composeTestRule.onNodeWithText("Visualizador").performClick()

        Espresso.closeSoftKeyboard()
        composeTestRule.onAllNodesWithText("Adicionar", useUnmergedTree = true).onLast().performClick()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Colaborador adicionado com sucesso!", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun teste03_adicionarEmailInvalido_InformaErroFormato() {
        composeTestRule.onNodeWithText("E-mail do usuário").performTextInput("email_invalido")

        Espresso.closeSoftKeyboard()
        composeTestRule.onAllNodesWithText("Adicionar", useUnmergedTree = true).onLast().performClick()

        composeTestRule.onNodeWithText("E-mail com formato inválido ou muito longo.").assertIsDisplayed()
    }

    @Test
    fun teste04_adicionarEmailNaoCadastrado_InformaErroUsuarioNaoEncontrado() {
        // Simulando que o backend retorna 404 para este email
        composeTestRule.onNodeWithText("E-mail do usuário").performTextInput("inexistente@teste.com")

        Espresso.closeSoftKeyboard()
        composeTestRule.onAllNodesWithText("Adicionar", useUnmergedTree = true).onLast().performClick()

        // Nota: A mensagem exata depende do handleError mapeando o 404 do repositório de acesso
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Usuário não encontrado", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun teste05_adicionarColaboradorJaExistente_InformaErro() {
        // Simulando que o backend retorna 409 para este email
        composeTestRule.onNodeWithText("E-mail do usuário").performTextInput("visualizador@teste.com")

        Espresso.closeSoftKeyboard()
        composeTestRule.onAllNodesWithText("Adicionar", useUnmergedTree = true).onLast().performClick()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("já faz parte", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /*@Test
    fun falhaConexaoAoAdicionarColaborador_InformaErroPadronizado() {
        composeTestRule.onNodeWithText("E-mail do usuário").performTextInput("timeout@teste.com")
        
        composeTestRule.onAllNodesWithText("Adicionar", useUnmergedTree = true).onLast().performClick()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Não foi possível se conectar ao servidor", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }*/
}
