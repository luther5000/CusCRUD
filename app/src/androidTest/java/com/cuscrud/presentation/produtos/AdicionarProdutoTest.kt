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
import kotlin.collections.isNotEmpty

@HiltAndroidTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AdicionarProdutoTest {

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

    private fun loginEIrParaInventario(ongName: String) {
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
            composeRule.onNodeWithText("Inventário Geral").isDisplayed()
        }
    }

    @Test
    fun test01_adicionarProdutoComoDono_ComSucesso() {
        loginEIrParaInventario("ONG A")
        
        abrirTelaAdicao()
        preencherFormularioValido("Arroz Dono", "Limpeza")
        clicarEmAdicionar()
        
        verificarMensagemSucesso()
        verificarProdutoNaLista("Limpeza", "Arroz Dono")
    }

    @Test
    fun test02_adicionarProdutoComoEditor_ComSucesso() {
        loginEIrParaInventario("ONG B") // João é Editor na ONG B
        
        abrirTelaAdicao()
        preencherFormularioValido("Arroz Editor", "Limpeza")
        clicarEmAdicionar()
        
        verificarMensagemSucesso()
        verificarProdutoNaLista("Limpeza", "Arroz Editor")
    }

    @Test
    fun test03_visualizadorNaoConsegueVerBotaoAdicionar() {
        loginEIrParaInventario("ONG C") // João é Visualizador na ONG C
        
        // 1. Verifica no Inventário Geral (FAB não deve existir)
        composeRule.onNodeWithContentDescription("Adicionar Produto").assertDoesNotExist()
        
        // 2. Entra em uma categoria existente (ex: Limpeza)
        composeRule.waitUntil(15000) {
            composeRule.onAllNodesWithText("Limpeza").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Limpeza", substring = true).performClick()
        
        // 3. Verifica na tela de produtos da categoria (FAB não deve existir)
        composeRule.onNodeWithContentDescription("Adicionar Produto").assertDoesNotExist()
    }

    @Test
    fun test04_adicionarProdutoPelaTelaDeCategoria_MantemTipoPorPadrao() {
        loginEIrParaInventario("ONG A")

        composeRule.waitUntil(15000) {
            composeRule.onAllNodesWithText("Limpeza").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Limpeza", substring = true).performClick()

        composeRule.waitUntil(15000) {
            composeRule.onAllNodesWithContentDescription("Adicionar Produto").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Adicionar Produto").performClick()

        composeRule.onNodeWithText("Limpeza").assertIsDisplayed()

        composeRule.onNodeWithText("Marca/Nome").performTextInput("Picanha")
        composeRule.onNodeWithText("Valor Unidade").performTextInput("60")
        composeRule.onNodeWithText("Medida").performClick()
        composeRule.onNodeWithText("kg").performClick()
        composeRule.onNodeWithText("Quantidade no Inventário").performTextInput("2")

        clicarEmAdicionar()

        verificarMensagemSucesso()
        composeRule.waitUntil(15000) {
            composeRule.onAllNodesWithText("Picanha").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Picanha").assertIsDisplayed()
    }

    @Test
    fun test05_validarCamposObrigatorios() {
        loginEIrParaInventario("ONG A")
        abrirTelaAdicao()
        clicarEmAdicionar()
        verificarMensagemErro("obrigatório")
    }

    @Test
    fun test06_validarLimiteCaracteresMarca() {
        loginEIrParaInventario("ONG A")
        abrirTelaAdicao()
        
        val nomeLongo = "A".repeat(260)
        preencherFormularioValido(nomeLongo, "Limpeza")

        clicarEmAdicionar()
        verificarMensagemErro("longo")
    }

    @Test
    fun test07_validarValorUnitarioLongo() {
        loginEIrParaInventario("ONG A")
        abrirTelaAdicao()
        preencherFormularioValido("Produto Teste", "Limpeza")

        composeRule.onNodeWithText("Valor Unidade").performTextReplacement("10000000000000000000")
        clicarEmAdicionar()
        verificarMensagemErro("Valor unitário muito grande.")

    }

    @Test
    fun test08_validarValorUnitarioNegativo() {
        loginEIrParaInventario("ONG A")
        abrirTelaAdicao()
        preencherFormularioValido("Produto Teste", "Limpeza")

        composeRule.onNodeWithText("Valor Unidade").performTextReplacement("-1")
        clicarEmAdicionar()
        verificarMensagemErro("Valor unitário inválido.")
    }

    @Test
    fun test09_validarQuantidadeLonga() {
        loginEIrParaInventario("ONG A")
        abrirTelaAdicao()
        preencherFormularioValido("Produto Teste", "Limpeza")

        composeRule.onNodeWithText("Quantidade no Inventário").performTextReplacement("10000000000000000000")
        clicarEmAdicionar()
        verificarMensagemErro("Quantidade muito grande.")
    }

    @Test
    fun test10_validarQuantidadeNegativa() {
        loginEIrParaInventario("ONG A")
        abrirTelaAdicao()
        preencherFormularioValido("Produto Teste", "Limpeza")

        composeRule.onNodeWithText("Quantidade no Inventário").performTextReplacement("-1")
        clicarEmAdicionar()
        verificarMensagemErro("Quantidade inválida.")
    }

    // Métodos Auxiliares
    private fun abrirTelaAdicao() {
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithContentDescription("Adicionar Produto").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Adicionar Produto").performClick()
    }

    private fun preencherFormularioValido(marca: String, tipo: String) {
        composeRule.onNodeWithText("Tipo").performClick()
        composeRule.onNodeWithText(tipo).performClick()
        composeRule.onNodeWithText("Marca/Nome").performTextInput(marca)
        composeRule.onNodeWithText("Valor Unidade").performTextInput("5")
        composeRule.onNodeWithText("Medida").performClick()
        composeRule.onNodeWithText("kg").performClick()
        composeRule.onNodeWithText("Quantidade no Inventário").performTextInput("10")
        Espresso.closeSoftKeyboard()
    }

    private fun clicarEmAdicionar() {
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText("Adicionar").performClick()
    }

    private fun verificarMensagemSucesso() {
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText("sucesso", substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun verificarProdutoNaLista(categoria: String, nome: String) {
        composeRule.waitUntil(15000) {
            composeRule.onAllNodesWithText(categoria).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText(categoria, substring = true).onFirst().performClick()
        
        composeRule.waitUntil(15000) {
            composeRule.onAllNodesWithText(nome).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(nome).assertIsDisplayed()
    }

    private fun verificarMensagemErro(termo: String) {
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText(termo, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
