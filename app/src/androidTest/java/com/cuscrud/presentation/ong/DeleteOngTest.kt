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
class DeleteOngTest {

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
        // Login João Novo
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
    }

    @Test
    fun test01_bloqueioRemocaoParaEditor_ONGB() {
        loginEIrParaConfiguracoes("ONG B") // João é Editor aqui

        // Tenta clicar no botão de remover (Ícone de lixeira)
        composeTestRule.onNodeWithContentDescription("Remover ONG").assertDoesNotExist()
    }

    @Test
    fun test02_bloqueioRemocaoParaVisualizador_ONGC() {
        loginEIrParaConfiguracoes("ONG C") // João é Visualizador aqui

        composeTestRule.onNodeWithContentDescription("Remover ONG").assertDoesNotExist()
    }

    @Test
    fun test03_cancelarRemocao_ONGremover() {
        loginEIrParaConfiguracoes("Remover") // João é Dono aqui

        composeTestRule.onNodeWithContentDescription("Remover ONG").performClick()

        // Verifica se o diálogo abriu
        composeTestRule.onNodeWithText("remover permanentemente a ONG", substring = true).assertIsDisplayed()

        // Clica em Cancelar
        composeTestRule.onNodeWithText("Cancelar").performClick()

        // Verifica se o diálogo sumiu e a ONG continua lá
        composeTestRule.onNodeWithText("Confirmar Exclusão").assertDoesNotExist()
        composeTestRule.onNodeWithText("Definições da ONG", substring = true).assertIsDisplayed()
    }

    @Test
    fun test04_removerComSucesso_ONGremover() {
        loginEIrParaConfiguracoes("Remover")

        composeTestRule.onNodeWithContentDescription("Remover ONG").performClick()

        // Clica em Confirmar/Remover no Diálogo
        composeTestRule.onAllNodesWithText("Remover").onLast().performClick()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Selecione sua ONG", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Remover", substring = true).isNotDisplayed()

    }
}