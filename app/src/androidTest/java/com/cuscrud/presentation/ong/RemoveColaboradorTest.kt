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
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import javax.inject.Inject

@HiltAndroidTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RemoveColaboradorTest {

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
    }

    private fun loginEIrParaConfiguracoes(ongName: String) {
        // Login João Novo (Dono da ONG A, Editor da ONG B, Visualizador da ONG C)
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("joao.novo@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senhaforte456")
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Seleciona a ONG específica
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText(ongName, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(ongName, substring = true).performClick()

        // Abre configurações
        composeTestRule.waitUntil(10000) {
            composeTestRule.onNodeWithContentDescription("Configurações da ONG").isDisplayed()
        }
        composeTestRule.onNodeWithContentDescription("Configurações da ONG").performClick()
        
        // Se o usuário for Dono, a seção Equipe deve aparecer
        if (ongName == "ONG A") {
            composeTestRule.waitUntil(10000) {
                composeTestRule.onAllNodesWithText("Equipe").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun test01_bloqueioVisualizacaoEquipeParaEditor_ONGB() {
        loginEIrParaConfiguracoes("ONG B") // João é Editor aqui

        // O cabeçalho "Equipe" e o botão "Adicionar" não devem aparecer
        composeTestRule.onNodeWithText("Equipe").assertDoesNotExist()
        composeTestRule.onNodeWithText("Adicionar").assertDoesNotExist()

        // Não deve ver o colaborador chamado "Remover"
        // Usamos correspondência exata para não confundir com o botão "Remover ONG"
        composeTestRule.onNodeWithText("Remover").assertDoesNotExist()
    }

    @Test
    fun test02_bloqueioVisualizacaoEquipeParaVisualizador_ONGC() {
        loginEIrParaConfiguracoes("ONG C") // João é Visualizador aqui

        composeTestRule.onNodeWithText("Equipe").assertDoesNotExist()
        composeTestRule.onNodeWithText("Adicionar").assertDoesNotExist()
        composeTestRule.onNodeWithText("Remover").assertDoesNotExist()
    }

    @Test
    fun test03_cancelarRemocaoColaborador() {
        loginEIrParaConfiguracoes("ONG A")

        // Aguarda o colaborador "Remover" aparecer
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Remover").fetchSemanticsNodes().isNotEmpty()
        }
        // Clica no colaborador "Remover" (usamos exact match para evitar o botão "Remover ONG")
        composeTestRule.onNodeWithText("Remover").performClick()

        // Inicia fluxo de remoção (abre o segundo diálogo por cima do primeiro)
        composeTestRule.onNodeWithText("Remover Colaborador").performClick()

        // Cancela no diálogo de confirmação
        composeTestRule.onNodeWithText("Confirmar Remoção").assertIsDisplayed()
        
        // Como existem dois botões "Cancelar" na árvore (um de cada diálogo), clicamos no último (o do topo)
        composeTestRule.onAllNodesWithText("Cancelar").onLast().performClick()

        // Verifica se o diálogo de confirmação fechou e o colaborador ainda está lá
        composeTestRule.onNodeWithText("Confirmar Remoção").assertDoesNotExist()
        
        // Verifica se o colaborador ainda está na lista. Usamos exact match.
        composeTestRule.onNodeWithText("Remover").assertIsDisplayed()
    }

    @Test
    fun test04_removerColaborador_ComSucesso() {
        loginEIrParaConfiguracoes("ONG A")

        // Aguarda o colaborador "Remover" aparecer
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Remover").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Remover").performClick()

        // Clica no botão de remoção dentro do diálogo de edição de papel
        composeTestRule.onNodeWithText("Remover Colaborador").performClick()

        // Confirma a remoção no diálogo de alerta
        composeTestRule.onNodeWithText("Confirmar Remoção").assertIsDisplayed()
        
        // Clica no botão "Remover" do diálogo de confirmação.
        // Pode haver dois "Remover" (o colaborador e o botão), então pegamos o último (o botão).
        composeTestRule.onAllNodesWithText("Remover").onLast().performClick()

        // Verifica mensagem de sucesso
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Colaborador removido com sucesso!").fetchSemanticsNodes().isNotEmpty()
        }

        // Verifica se o colaborador saiu da lista
        composeTestRule.onNodeWithText("Remover").assertDoesNotExist()
    }

    /*@Test
    fun test05_falhaConexaoAoRemoverColaborador_InformaErro() {
        loginEIrParaConfiguracoes("ONG A")

        // Usando o mock que causa falha
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Colaborador Falha", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Colaborador Falha", substring = true).performClick()

        composeTestRule.onNodeWithText("Remover Colaborador").performClick()
        
        // Confirma no diálogo de confirmação
        // "Remover" botão do diálogo é o último adicionado
        composeTestRule.onAllNodesWithText("Remover").onLast().performClick()

        // Verifica mensagem de erro
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Não foi possível comunicar com o servidor", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Garante que o colaborador NÃO foi removido da lista
        composeTestRule.onNodeWithText("Colaborador Falha", substring = true).assertIsDisplayed()
    }*/
}
