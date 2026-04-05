package com.cuscrud.presentation.ong

import androidx.compose.ui.test.*import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
class EditOngTest {

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
        // Estado inicial limpo
        runBlocking {
            authRepository.logout()
            inventoryRepository.clearActiveInventory()
        }
    }

    private fun loginEIrParaConfiguracoes(user: String = "joao.novo@example.com") {
        // Login
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput(user)
        composeTestRule.onNodeWithText("Senha").performTextInput("senhaforte456")
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Seleciona ONG
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("ONG A", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("ONG A", substring = true).performClick()

        // Vai para Configurações
        composeTestRule.waitUntil(10000) {
            composeTestRule.onNodeWithContentDescription("Configurações da ONG").isDisplayed()
        }
        composeTestRule.onNodeWithContentDescription("Configurações da ONG").performClick()
    }

    @Test
    fun teste01_editarNomeComSucesso() {
        loginEIrParaConfiguracoes()

        composeTestRule.onNodeWithContentDescription("Editar").performClick()

        // Limpa e digita novo nome
        composeTestRule.onNodeWithText("Nome da ONG").performTextReplacement("ONG A Alterada")

        // Clica no botão Salvar (ícone de Check)
        composeTestRule.onNodeWithContentDescription("Salvar").performClick()

        // Verifica mensagem de sucesso e atualização na tela
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("atualizado com sucesso", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("ONG A Alterada", substring = true).assertIsDisplayed()
    }

    @Test
    fun teste02_tentativaEdicaoNomeEmBranco_ExibeErro() {
        loginEIrParaConfiguracoes()

        composeTestRule.onNodeWithContentDescription("Editar").performClick()

        // Deixa o campo vazio
        composeTestRule.onNodeWithText("Nome da ONG").performTextReplacement("")

        composeTestRule.onNodeWithContentDescription("Salvar").performClick()

        // Verifica mensagem de validação
        composeTestRule.onNodeWithText("O preenchimento do nome é obrigatório.").assertIsDisplayed()
    }

    @Test
    fun teste03_tentativaEdicaoNomeMuitoLongo_ExibeErro() {
        loginEIrParaConfiguracoes()

        composeTestRule.onNodeWithContentDescription("Editar").performClick()

        // Gera string com 256 caracteres
        val nomeLongo = "A".repeat(256)
        composeTestRule.onNodeWithText("Nome da ONG").performTextReplacement(nomeLongo)

        composeTestRule.onNodeWithContentDescription("Salvar").performClick()

        // Verifica mensagem de limite
        composeTestRule.onNodeWithText("O nome da ONG não pode ultrapassar 255 caracteres.").assertIsDisplayed()
    }

    @Test
    fun teste04_bloqueioEdicaoParaNaoDonos() {
        // Assume-se que 'colaborador@teste.com' foi criado como Editor anteriormente
        loginEIrParaConfiguracoes("colaborador.teste@example.com")

        // Para um Editor, o botão de edição (ícone de lápis) não deve ser exibido
        // conforme a lógica: if (uiState.userRole.canManageInventory()) no Scaffold
        composeTestRule.onNodeWithContentDescription("Editar").assertDoesNotExist()
    }
}