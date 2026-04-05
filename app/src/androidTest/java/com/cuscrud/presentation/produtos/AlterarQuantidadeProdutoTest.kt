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
class AlterarQuantidadeProdutoTest {

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
        // Login João Novo (Dono da ONG A, Editor da ONG B, Visualizador da ONG C)
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
    }

    @Test
    fun test01_aumentarEmUmAQuantidadeDeItensDoProduto_ComoDono() {
        loginEIrParaProdutos("ONG A", "Carnes")
        
        // Dado que o usuário visualiza um produto
        composeRule.onNodeWithText("10").assertIsDisplayed()
        
        // Quando o usuário clica na opção de aumentar em um a quantidade de itens
        composeRule.onAllNodesWithContentDescription("Aumentar").onFirst().performClick()
        
        // Então o usuário visualiza que existe um item a mais do produto
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("11").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("11").assertIsDisplayed()
    }

    @Test
    fun test02_diminuirEmUmAQuantidadeDeItensDoProduto_ComoDono() {
        loginEIrParaProdutos("ONG A", "Carnes")
        
        // Dado que o usuário visualiza um produto
        composeRule.onNodeWithText("11").assertIsDisplayed()
        
        // Quando o usuário clica na opção de diminuir em um a quantidade de itens
        composeRule.onAllNodesWithContentDescription("Diminuir").onFirst().performClick()
        
        // Então o usuário visualiza que existe um item a menos do produto
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("10").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("10").assertIsDisplayed()
    }

    @Test
    fun test03_diminuirEmUmAQuantidadeDeItensDeUmProdutoComZeroItens_ComoDono() {
        loginEIrParaProdutos("ONG A", "Carnes")
        
        // Dado que o produto possui zero itens
        repeat(10) {
            composeRule.onAllNodesWithContentDescription("Diminuir").onFirst().performClick()
        }
        
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("0").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("0").assertIsDisplayed()
        
        // Quando o usuário clica na opção de diminuir em um a quantidade itens
        composeRule.onAllNodesWithContentDescription("Diminuir").onFirst().performClick()
        
        // Então o usuário visualiza que a quantidade de itens não foi alterada
        composeRule.onNodeWithText("0").assertIsDisplayed()
        
        // E recebe a mensagem de erro via Snackbar
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("não é possível alterar o produto para menos de 0", substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun test04_editorConsegueAlterarQuantidade() {
        loginEIrParaProdutos("ONG B", "Carnes") // João é Editor na ONG B
        
        // Verifica se os botões estão presentes e funcionam
        composeRule.onNodeWithText("10").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Aumentar").onFirst().performClick()
        
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("11").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("11").assertIsDisplayed()
        
        composeRule.onAllNodesWithContentDescription("Diminuir").onFirst().performClick()
        
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("10").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("10").assertIsDisplayed()
    }

    @Test
    fun test05_visualizadorNaoConsegueVerBotoesDeAlteracao() {
        loginEIrParaProdutos("ONG C", "Carnes") // João é Visualizador na ONG C
        
        // Aguarda o carregamento da lista
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("Qtd:", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Então o usuário não deve visualizar as opções de aumentar ou diminuir quantidade
        composeRule.onAllNodesWithContentDescription("Aumentar").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Diminuir").assertCountEquals(0)
        
        // A quantidade deve estar visível no formato informativo para visualizadores
        composeRule.onNodeWithText("Qtd: 10").assertIsDisplayed()
    }
}
