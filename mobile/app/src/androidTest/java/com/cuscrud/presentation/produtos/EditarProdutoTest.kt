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
class EditarProdutoTest {

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
    }

    @Test
    fun test01_editarProdutoComoDono_ComSucesso() {
        loginEIrParaProdutos("ONG A", "Limpeza")
        
        // 1. Clica no primeiro produto da lista para abrir Detalhes
        composeRule.onAllNodesWithTag("produto_item").onFirst().performClick()
        
        // 2. Na tela de detalhes, clica no botão de editar
        composeRule.onNodeWithContentDescription("Editar Produto").performClick()
        
        // 3. Altera a marca e confirma
        composeRule.onNodeWithText("Marca/Nome").performTextReplacement("Frango Editado Dono")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText("Confirmar").performClick()
        
        // 4. Verifica sucesso e se está na tela de Detalhes com o novo nome
        verificarMensagemSucesso()
        composeRule.onNodeWithText("Frango Editado Dono").assertIsDisplayed()
        
        // 5. Volta para a lista e verifica se o nome também atualizou lá (Refresh ON_RESUME)
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.onNodeWithText("Frango Editado Dono").assertIsDisplayed()
    }

    @Test
    fun test02_editarProdutoComoEditor_ComSucesso() {
        loginEIrParaProdutos("ONG B", "Limpeza") // João é Editor na ONG B
        
        composeRule.onAllNodesWithTag("produto_item").onFirst().performClick()
        
        composeRule.onNodeWithContentDescription("Editar Produto").performClick()
        composeRule.onNodeWithText("Marca/Nome").performTextReplacement("Frango Editado Editor")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText("Confirmar").performClick()
        
        verificarMensagemSucesso()
        composeRule.onNodeWithText("Frango Editado Editor").assertIsDisplayed()
        
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.onNodeWithText("Frango Editado Editor").assertIsDisplayed()
    }

    @Test
    fun test03_visualizadorNaoConsegueVerBotaoEditar() {
        loginEIrParaProdutos("ONG C", "Limpeza") // João é Visualizador na ONG C
        
        // Visualizador não deve ver botões de ajuste de quantidade na lista
        composeRule.onAllNodesWithContentDescription("Aumentar").assertCountEquals(0)
        
        // Entra no detalhe do produto
        composeRule.onAllNodesWithTag("produto_item").onFirst().performClick()
        
        // Verifica se o botão de editar (FAB) não existe na tela de detalhes
        composeRule.onNodeWithContentDescription("Editar Produto").assertDoesNotExist()
    }

    @Test
    fun test04_validarCamposObrigatoriosNaEdicao() {
        loginEIrParaProdutos("ONG A", "Limpeza")
        
        composeRule.onAllNodesWithTag("produto_item").onFirst().performClick()
        composeRule.onNodeWithContentDescription("Editar Produto").performClick()
        
        composeRule.onNodeWithText("Marca/Nome").performTextReplacement("")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText("Confirmar").performClick()
        
        verificarMensagemErro("obrigatório")
    }

    private fun verificarMensagemSucesso() {
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("sucesso", substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun verificarMensagemErro(termo: String) {
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText(termo, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
