package com.cuscrud.presentation.produtos

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import com.cuscrud.MainActivity
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.ProdutoRepository
import com.cuscrud.domain.repository.TipoRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EditarProdutoTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var tipoRepository: TipoRepository

    @Inject
    lateinit var produtoRepository: ProdutoRepository

    private val tipoTeste = Tipo(id = 888, nome = "Laticínios", imagem = byteArrayOf(0))
    private val produtoNomeInicial = "Leite Integral"
    private val produtoNomeEditado = "Leite Desnatado"

    @Before
    fun setup() {
        hiltRule.inject()
        runBlocking {
            tipoRepository.insertTipo(tipoTeste)
            produtoRepository.insertProduto(
                Produto(
                    id = 0,
                    tipo = tipoTeste,
                    marca = produtoNomeInicial,
                    dataValidade = Date(),
                    unidade = 1,
                    unidadeMedida = "l",
                    quantidade = 5
                )
            )
        }
    }

    @Test
    fun edicaoDoProdutoComSucesso() {
        irParaDetalhesDoProduto(produtoNomeInicial)
        clicarEmEditar()
        alterarNomeProduto(produtoNomeEditado)
        clicarEmConfirmar()
        verificarMensagemSucesso()
        verificarVoltaParaInventario()
        verificarProdutoNaLista(tipoTeste.nome, produtoNomeEditado)
    }

    @Test
    fun cancelarAcao() {
        irParaDetalhesDoProduto(produtoNomeInicial)
        clicarEmEditar()
        alterarNomeProduto(produtoNomeEditado)
        clicarEmCancelar()
        confirmarCancelamento()
        clicarEmVoltar()
        clicarEmVoltar()
        verificarVoltaParaInventario()
        verificarProdutoNaLista(tipoTeste.nome, produtoNomeInicial)
    }

    @Test
    fun camposObrigatoriosEmBranco() {
        irParaDetalhesDoProduto(produtoNomeInicial)
        clicarEmEditar()
        deixarMarcaEmBranco()
        clicarEmConfirmar()
        verificarMensagemErro("preencher todos os campos obrigatórios")
    }

    @Test
    fun quantidadeNegativa() {
        irParaDetalhesDoProduto(produtoNomeInicial)
        clicarEmEditar()
        inserirQuantidadeNegativa()
        clicarEmConfirmar()
        verificarMensagemErro("quantidade positiva")
    }

    @Test
    fun erroInternoDoSistema() {
        irParaDetalhesDoProduto(produtoNomeInicial)
        clicarEmEditar()
        deixarMarcaEmBranco() // Simula erro de validação/persistência
        clicarEmConfirmar()
        verificarPermanenciaNaTelaEdicao()
        verificarMensagemErro("preencher todos os campos obrigatórios")
        verificarValorNoCampo(produtoNomeEditado, false) // Verifica se o que ele inseriu (vazio no caso) se mantém ou mostra erro
    }

    // =========================================================
    // DSL de Teste
    // =========================================================

    private fun irParaDetalhesDoProduto(nome: String) {
        composeRule.waitUntil(15000) {
            composeRule.onAllNodesWithText(tipoTeste.nome, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        // Clica na categoria
        composeRule.onNodeWithText(tipoTeste.nome, ignoreCase = true).performClick()
        
        // Clica no produto
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText(nome, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(nome, ignoreCase = true).performClick()
    }

    private fun clicarEmEditar() {
        composeRule.onNodeWithContentDescription("Editar Produto").performClick()
    }

    private fun alterarNomeProduto(novoNome: String) {
        composeRule.onNodeWithText("Marca/Nome", ignoreCase = true)
            .performTextReplacement(novoNome)

        // Força o fechamento do teclado
        Espresso.closeSoftKeyboard()
    }

    private fun clicarEmConfirmar() {
        composeRule.onNodeWithText("Confirmar", ignoreCase = true).performClick()
    }

    private fun clicarEmCancelar() {
        composeRule.onNodeWithText("Cancelar", ignoreCase = true).performClick()
    }

    private fun clicarEmVoltar() {
        composeRule.onNodeWithContentDescription("Voltar", ignoreCase = true).performClick()
    }

    private fun confirmarCancelamento() {
        composeRule.onAllNodesWithText("Confirmar", ignoreCase = true)
            .onLast()
            .performClick()
    }

    private fun deixarMarcaEmBranco() {
        composeRule.onNodeWithText("Marca/Nome", ignoreCase = true).performTextReplacement("")
    }

    private fun inserirQuantidadeNegativa() {
        composeRule.onNodeWithText("Quantidade no Inventário", ignoreCase = true).performTextReplacement("-5")
    }

    private fun verificarMensagemSucesso() {
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("sucesso", ignoreCase = true, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun verificarVoltaParaInventario() {
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("Inventário Geral", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun verificarProdutoNaLista(categoria: String, nome: String) {
        // Se já estiver no inventário, clica na categoria se necessário
        composeRule.onNodeWithText(categoria, ignoreCase = true).performClick()
        composeRule.onNodeWithText(nome, ignoreCase = true).assertIsDisplayed()
    }

    private fun verificarMensagemErro(termo: String) {
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText(termo, ignoreCase = true, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText(termo, ignoreCase = true, substring = true)
            .onLast()
            .assertIsDisplayed()
    }

    private fun verificarPermanenciaNaTelaEdicao() {
        composeRule.onNodeWithText("Editar Produto", ignoreCase = true).assertIsDisplayed()
    }

    private fun verificarValorNoCampo(valor: String, exists: Boolean) {
        if (exists) {
            composeRule.onNodeWithText(valor, ignoreCase = true).assertExists()
        } else {
            // Se for vazio
            composeRule.onNodeWithText("Marca/Nome", ignoreCase = true).assert(hasText(""))
        }
    }
}
