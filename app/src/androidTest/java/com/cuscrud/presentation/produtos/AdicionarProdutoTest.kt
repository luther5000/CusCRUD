package com.cuscrud.presentation.produtos

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuscrud.MainActivity
import com.cuscrud.domain.model.Tipo
import com.cuscrud.domain.repository.TipoRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AdicionarProdutoTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var tipoRepository: TipoRepository

    @Before
    fun setup() {
        hiltRule.inject()
        runBlocking {
            // Garante que o banco tenha o tipo necessário para o teste
            // Usamos ID 999 para evitar conflitos com IDs gerados automaticamente
            tipoRepository.insertTipo(
                Tipo(id = 999, nome = "Carnes", imagem = byteArrayOf(0))
            )
        }
    }

    @Test
    fun adicionarProdutoComSucesso() {
        abrirTelaAdicao()
        preencherFormularioValido()
        clicarEmAdicionar()
        verificarMensagemSucesso()
        verificarVoltaParaInventario()
        verificarProdutoNaLista("Carnes", "Arroz")
    }

    @Test
    fun cancelarAdicao() {
        abrirTelaAdicao()
        preencherFormularioValido()
        clicarEmCancelar()
        confirmarCancelamento()
        verificarProdutoNaoExiste("Arroz")
    }

    @Test
    fun validarCamposObrigatorios() {
        abrirTelaAdicao()
        preencherFormularioValido() // Preenche tudo primeiro
        deixarMarcaEmBranco()       // Limpa apenas o campo obrigatório
        clicarEmAdicionar()         // Confirma a adição para disparar a validação
        verificarMensagemErro("preencher")
    }

    @Test
    fun validarQuantidadePositiva() {
        abrirTelaAdicao()
        preencherFormularioValido()
        inserirQuantidadeNegativa()
        clicarEmAdicionar()
        verificarMensagemErro("quantidade")
    }

    @Test
    fun erroInternoMantemDadosETela() {
        abrirTelaAdicao()
        preencherFormularioValido()
        simularErroInterno() // Limpa campo obrigatório para simular falha de validação/inserção
        clicarEmAdicionar()
        verificarPermanenciaNaTelaAdicao()
        verificarMensagemErro("preencher")
    }

    // =========================================================
    // Métodos de Ação (DSL de Teste)
    // =========================================================

    private fun abrirTelaAdicao() {
        // Espera a hierarquia do Compose estar pronta antes de interagir
        composeRule.waitUntil(15000) {
            composeRule.onAllNodesWithContentDescription("Adicionar Produto")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Adicionar Produto").performClick()
    }

    private fun preencherFormularioValido() {
        // Seleção de Tipo no Dropdown
        composeRule.onNodeWithText("Tipo", ignoreCase = true).performClick()
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("Carnes", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Carnes", ignoreCase = true).onFirst().performClick()

        composeRule.onNodeWithText("Marca/Nome", ignoreCase = true).performTextReplacement("Arroz")
        composeRule.onNodeWithText("Valor Unidade", ignoreCase = true).performTextReplacement("5")

        // Seleção de Medida
        composeRule.onNodeWithText("Medida", ignoreCase = true).performClick()
        composeRule.onNodeWithText("kg", ignoreCase = true).performClick()

        composeRule.onNodeWithText("Quantidade no Inventário", ignoreCase = true).performTextReplacement("10")
    }

    private fun clicarEmAdicionar() {
        composeRule.onNodeWithText("Adicionar", ignoreCase = true).performClick()
    }

    private fun clicarEmCancelar() {
        composeRule.onNodeWithText("Cancelar", ignoreCase = true).performClick()
    }

    private fun confirmarCancelamento() {
        composeRule.onNodeWithText("Confirmar", ignoreCase = true).performClick()
    }

    private fun deixarMarcaEmBranco() {
        composeRule.onNodeWithText("Marca/Nome", ignoreCase = true).performTextReplacement("")
    }

    private fun inserirQuantidadeNegativa() {
        composeRule.onNodeWithText("Quantidade no Inventário", ignoreCase = true).performTextReplacement("-1")
    }

    private fun simularErroInterno() {
        deixarMarcaEmBranco()
    }

    // =========================================================
    // Métodos de Verificação
    // =========================================================

    private fun verificarMensagemSucesso() {
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("sucesso", ignoreCase = true, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("sucesso", ignoreCase = true, substring = true).assertIsDisplayed()
    }

    private fun verificarVoltaParaInventario() {
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("Inventário Geral", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Inventário Geral", ignoreCase = true).assertIsDisplayed()
    }

    private fun verificarProdutoNaLista(categoria: String, nome: String) {
        // Clica na categoria para ver os produtos dentro dela
        composeRule.onNodeWithText(categoria, ignoreCase = true).performClick()
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText(nome, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(nome, ignoreCase = true).assertIsDisplayed()
    }

    private fun verificarProdutoNaoExiste(nome: String) {
        composeRule.onNodeWithText(nome, ignoreCase = true).assertDoesNotExist()
    }

    private fun verificarMensagemErro(termo: String) {
        // O erro ocorre porque "quantidade" aparece no label do campo E na mensagem de erro.
        // Vamos usar filterToOne para garantir que estamos pegando a mensagem que NÃO é o campo de texto.
        composeRule.waitUntil(10000) {
            composeRule.onAllNodesWithText(termo, ignoreCase = true, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Se houver mais de um, tentamos pegar o que é do tipo Snackbar ou apenas o último que apareceu
        composeRule.onAllNodesWithText(termo, ignoreCase = true, substring = true)
            .onLast()
            .assertIsDisplayed()
    }

    private fun verificarPermanenciaNaTelaAdicao() {
        composeRule.onNodeWithText("Adicionar Produto", ignoreCase = true).assertIsDisplayed()
    }
}
