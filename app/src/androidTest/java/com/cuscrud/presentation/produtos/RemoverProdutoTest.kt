package com.cuscrud.presentation.produtos

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
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import javax.inject.Inject

@HiltAndroidTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RemoverProdutoTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

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
    }

    private fun loginEIrParaProdutos(ongName: String, categoria: String) {
        // Login João Novo (Dono A, Editor B, Visualizador C)
        composeRule.onNodeWithText("E-mail ou Login").performTextInput("joao.novo@example.com")
        composeRule.onNodeWithText("Senha").performTextInput("senhaforte456")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText("ENTRAR").performClick()

        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText(ongName, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(ongName, substring = true).performClick()

        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText(categoria, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(categoria, substring = true).performClick()
        
        // Aguarda a lista de produtos carregar
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithTag("produto_item").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun test01_cancelarExclusaoProduto() {
        loginEIrParaProdutos("ONG A", "Carnes")

        // 1. Entra na tela de detalhes
        composeRule.onAllNodesWithTag("produto_item").onFirst().performClick()

        // 2. Clica no ícone de lixeira (Excluir Produto) na TopAppBar
        composeRule.onNodeWithContentDescription("Excluir Produto").performClick()
        
        // 3. Clica em cancelar no diálogo
        composeRule.onNodeWithText("Cancelar").performClick()

        // O diálogo deve fechar e a tela de detalhes deve continuar visível
        composeRule.onNodeWithText("Excluir Produto").assertDoesNotExist()
        composeRule.onNodeWithText("Detalhes do Produto").assertIsDisplayed()
    }

    @Test
    fun test02_removerProdutoComoDono_ComSucesso() {
        loginEIrParaProdutos("ONG A", "Carnes")
        
        // 1. Entra na tela de detalhes
        composeRule.onAllNodesWithText("Picanha").onFirst().performClick()
        
        // 2. Clica no ícone de lixeira (Excluir Produto) na TopAppBar
        composeRule.onNodeWithContentDescription("Excluir Produto").performClick()
        // 3. Confirma no diálogo
        composeRule.onNodeWithText("Excluir").performClick()

        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("Picanha").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Picanha").assertIsNotDisplayed()
    }

    @Test
    fun test03_removerProdutoComoEditor_ComSucesso() {
        loginEIrParaProdutos("ONG B", "Carnes")
        
        composeRule.onAllNodesWithText("Picanha").onFirst().performClick()
        composeRule.onNodeWithContentDescription("Excluir Produto").performClick()
        composeRule.onNodeWithText("Excluir").performClick()

        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("Picanha").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Picanha").assertIsNotDisplayed()
    }

    @Test
    fun test04_visualizadorNaoConsegueVerBotaoExcluir() {
        loginEIrParaProdutos("ONG C", "Carnes")
        
        // 1. Na lista não deve haver botão de excluir (mesmo que antigo)
        composeRule.onAllNodesWithContentDescription("Excluir").assertCountEquals(0)
        
        // 2. Entra na tela de detalhes
        composeRule.onAllNodesWithTag("produto_item").onFirst().performClick()
        
        // 3. O ícone de lixeira não deve existir na TopAppBar para visualizadores
        composeRule.onNodeWithContentDescription("Excluir Produto").assertDoesNotExist()
    }

    private fun verificarMensagemSucesso() {
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("sucesso", substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
