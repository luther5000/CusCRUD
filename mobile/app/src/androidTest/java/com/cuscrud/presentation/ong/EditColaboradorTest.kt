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
class EditColaboradorTest {

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
    fun test01_alterarPapelDeVisualizadorParaEditor_ComSucesso() {
        loginEIrParaConfiguracoes("ONG A")

        // Aguarda o colaborador aparecer na lista antes de clicar
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Colaborador Teste", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Colaborador Teste", substring = true).performClick()

        // Verifica se o diálogo abriu e seleciona Editor
        composeTestRule.onNodeWithText("Editar Permissão").assertIsDisplayed()
        
        // Seleciona a opção "Editor" dentro do diálogo (usando onLast se houver duplicatas no diálogo)
        composeTestRule.onAllNodesWithText("Editor").onLast().performClick()
        
        // Confirma
        composeTestRule.onNodeWithText("Salvar").performClick()

        // Verifica mensagem de sucesso
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Permissão atualizada com sucesso!").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Verifica se o Badge na lista mudou para EDITOR. 
        // Como pode haver múltiplos "EDITOR" na tela, verificamos se existe pelo menos um nó com esse texto.
        composeTestRule.onAllNodesWithText("EDITOR").onFirst().assertIsDisplayed()
    }

    @Test
    fun test02_alterarPapelDeEditorParaVisualizador_ComSucesso() {
        loginEIrParaConfiguracoes("ONG A")

        // Aguarda o colaborador aparecer
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Colaborador Teste", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Colaborador Teste", substring = true).performClick()

        // Seleciona a opção "Visualizador"
        composeTestRule.onAllNodesWithText("Visualizador").onLast().performClick()
        composeTestRule.onNodeWithText("Salvar").performClick()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Permissão atualizada com sucesso!").fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onAllNodesWithText("READER").onFirst().assertIsDisplayed()
    }

    @Test
    fun test03_bloqueioVisualizacaoEquipeParaEditor_ONGB() {
        loginEIrParaConfiguracoes("ONG B") // João é Editor aqui

        // O cabeçalho "Equipe" e o botão "Adicionar" não devem aparecer para Editores
        composeTestRule.onNodeWithText("Equipe").assertDoesNotExist()
        composeTestRule.onNodeWithText("Adicionar").assertDoesNotExist()
        
        // Não deve ver a lista de colaboradores
        composeTestRule.onNodeWithText("Colaborador Teste", substring = true).assertDoesNotExist()
    }

    @Test
    fun test04_bloqueioVisualizacaoEquipeParaVisualizador_ONGC() {
        loginEIrParaConfiguracoes("ONG C") // João é Visualizador aqui

        composeTestRule.onNodeWithText("Equipe").assertDoesNotExist()
        composeTestRule.onNodeWithText("Adicionar").assertDoesNotExist()
        composeTestRule.onNodeWithText("Colaborador Teste", substring = true).assertDoesNotExist()
    }

    /*@Test
    fun test05_falhaConexaoAoAlterarPermissao_InformaErro() {
        loginEIrParaConfiguracoes("ONG A")

        // Aguarda o colaborador de falha aparecer
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Colaborador Falha", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithText("Colaborador Falha", substring = true).performClick()
        composeTestRule.onAllNodesWithText("Editor").onLast().performClick()
        composeTestRule.onNodeWithText("Salvar").performClick()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Não foi possível comunicar com o servidor", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }*/
}
