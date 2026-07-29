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
class AddTipoDuranteProdutoTest {

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

    private fun loginEIrParaAdicionarProduto() {
        composeRule.onNodeWithText("E-mail ou Login").performTextInput("joao.novo@example.com")
        composeRule.onNodeWithText("Senha").performTextInput("senhaforte456")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText("ENTRAR").performClick()

        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("ONG A", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("ONG A", substring = true).performClick()

        composeRule.waitUntil(10000) {
            composeRule.onNodeWithContentDescription("Adicionar Produto").isDisplayed()
        }
        composeRule.onNodeWithContentDescription("Adicionar Produto").performClick()
    }

    @Test
    fun test01_criarNovaCategoriaComSucesso() {
        loginEIrParaAdicionarProduto()

        composeRule.onNodeWithText("Tipo").performClick()
        composeRule.onNodeWithText("Adicionar Nova Categoria", substring = true).performClick()

        composeRule.onNodeWithText("Nome da Categoria").performTextInput("Limpeza")
        composeRule.onNodeWithText("Criar").performClick()

        // Aguarda a categoria ser criada, selecionada e o diálogo fechar
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("Limpeza").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Verifica se a categoria aparece como selecionada no formulário
        composeRule.onNodeWithText("Limpeza").assertIsDisplayed()
        
        // Verifica mensagem de sucesso no Snackbar
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("criada com sucesso", substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun test02_validarNomeCategoriaObrigatorio() {
        loginEIrParaAdicionarProduto()

        composeRule.onNodeWithText("Tipo").performClick()
        composeRule.onNodeWithText("Adicionar Nova Categoria", substring = true).performClick()

        // Tenta criar sem nome
        composeRule.onNodeWithText("Criar").performClick()

        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("nome da categoria é obrigatório", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun test03_validarLimiteCaracteresCategoria() {
        loginEIrParaAdicionarProduto()

        composeRule.onNodeWithText("Tipo").performClick()
        composeRule.onNodeWithText("Adicionar Nova Categoria", substring = true).performClick()

        // Nome com 256 caracteres
        val nomeLongo = "C".repeat(256)
        composeRule.onNodeWithText("Nome da Categoria").performTextInput(nomeLongo)
        composeRule.onNodeWithText("Criar").performClick()

        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("muito longo", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun test04_validarCategoriaDuplicada() {
        loginEIrParaAdicionarProduto()

        composeRule.onNodeWithText("Tipo").performClick()
        
        // "Carnes" já existe no mock/setup padrão para a ONG A
        composeRule.onNodeWithText("Adicionar Nova Categoria", substring = true).performClick()

        composeRule.onNodeWithText("Nome da Categoria").performTextInput("Limpeza")
        composeRule.onNodeWithText("Criar").performClick()

        // O backend (mock) deve retornar 409 Conflict, que o RemoteTipoRepository mapeia.
        // Se o mock não estiver configurado para 409 com o nome "Carnes", o teste pode falhar.
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("já existe", substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
