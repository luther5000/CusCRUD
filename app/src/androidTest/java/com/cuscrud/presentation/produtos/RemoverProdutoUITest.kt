package com.cuscrud.presentation.produtos

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class RemoverProdutoUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var tipoRepository: TipoRepository

    @Inject
    lateinit var produtoRepository: ProdutoRepository

    private val tipoTeste = Tipo(id = 100, nome = "Limpeza", imagem = byteArrayOf(0))
    private val produtoTeste = Produto(
        id = 500,
        tipo = tipoTeste,
        marca = "Detergente",
        dataValidade = Date(),
        unidade = 1,
        unidadeMedida = "L",
        quantidade = 5
    )

    @Before
    fun setup() {
        hiltRule.inject()
        runBlocking {
            tipoRepository.insertTipo(tipoTeste)
            produtoRepository.insertProduto(produtoTeste)
        }
    }

    @Test
    fun cancelarARemocaoDoProduto() {
        navegarParaTelaDeProdutos()
        clicarNoIconeDeRemover(produtoTeste.marca)
        verificarDialogoDeConfirmacaoVisivel(produtoTeste.marca)
        clicarEmCancelarRemocao()
        verificarDialogoNaoVisivel()
        verificarProdutoVisivelNaLista(produtoTeste.marca)
    }

    @Test
    fun confirmarARemocaoDoProdutoComSucesso() {
        navegarParaTelaDeProdutos()
        clicarNoIconeDeRemover(produtoTeste.marca)
        clicarEmConfirmarRemocao()
        
        // Refatorado para verificar a mensagem dinâmica baseada na marca do produto deletado
        verificarMensagemDeSucesso("${produtoTeste.marca} removido com sucesso")
        
        verificarPermanenciaNaTelaDeProdutos()
        verificarProdutoNaoVisivelNaLista(produtoTeste.marca)
    }

    // =========================================================
    // Métodos de Ação (DSL de Teste)
    // =========================================================

    private fun navegarParaTelaDeProdutos() {
        // Na tela de Inventário, clica na categoria para abrir a lista de produtos
        composeRule.onNodeWithText(tipoTeste.nome, ignoreCase = true).performClick()
        
        // Espera carregar a tela de produtos
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("Produtos da Categoria", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun clicarNoIconeDeRemover(nomeDoProduto: String) {
        // Encontra o botão de remover que compartilha o mesmo item (Card/Row) que o texto do produto.
        composeRule.onNode(
            hasContentDescription("Remover Produto") and
                    hasAnyAncestor(hasText(nomeDoProduto, ignoreCase = true))
        ).performClick()
    }

    private fun clicarEmConfirmarRemocao() {
        composeRule.onNodeWithText("Sim", ignoreCase = true).performClick()
    }

    private fun clicarEmCancelarRemocao() {
        composeRule.onNodeWithText("Não", ignoreCase = true).performClick()
    }

    // =========================================================
    // Métodos de Verificação
    // =========================================================

    private fun verificarDialogoDeConfirmacaoVisivel(nomeDoProduto: String) {
        composeRule.onNodeWithText("Confirmar Remoção", ignoreCase = true).assertIsDisplayed()
        composeRule.onNodeWithText("Deseja realmente remover $nomeDoProduto?", ignoreCase = true).assertIsDisplayed()
    }

    private fun verificarDialogoNaoVisivel() {
        composeRule.onNodeWithText("Confirmar Remoção", ignoreCase = true).assertDoesNotExist()
    }

    private fun verificarProdutoVisivelNaLista(nomeDoProduto: String) {
        composeRule.onNodeWithText(nomeDoProduto, ignoreCase = true).assertIsDisplayed()
    }

    private fun verificarProdutoNaoVisivelNaLista(nomeDoProduto: String) {
        composeRule.onNodeWithText(nomeDoProduto, ignoreCase = true).assertDoesNotExist()
    }

    private fun verificarMensagemDeSucesso(mensagem: String) {
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText(mensagem, ignoreCase = true, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(mensagem, ignoreCase = true, substring = true).assertIsDisplayed()
    }

    private fun verificarPermanenciaNaTelaDeProdutos() {
        composeRule.onNodeWithText("Produtos da Categoria", ignoreCase = true).assertIsDisplayed()
    }
}
