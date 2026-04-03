package com.cuscrud.presentation.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import com.cuscrud.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LoginTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun loginSucesso_RedirecionaParaSelecaoDeOng() {
        // Dado que o usuário está na tela de login
        
        // Quando ele insere seu email e senha corretos
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("joao.novo@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senhaforte456")

        // Fecha o teclado explicitamente para evitar que ele cubra o botão ENTRAR
        Espresso.closeSoftKeyboard()

        // E clica no botão de entrar
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema autentica o usuário e redireciona
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Selecione sua ONG", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithText("Selecione sua ONG", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun loginCredenciaisIncorretas_ExibeMensagemErro() {
        // Quando o usuário insere um email ou senha incorretos
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("errado")
        composeTestRule.onNodeWithText("Senha").performTextInput("senha_errada")
        
        Espresso.closeSoftKeyboard()

        // E clica no botão de entrar
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema exibe uma mensagem de erro
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Credenciais inválidas", ignoreCase = true, substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("servidor", ignoreCase = true, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun loginCamposEmBranco_InformaPreenchimentoObrigatorio() {
        // Quando o usuário deixa o campo de email ou senha em branco
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema informa que é necessário preencher todos os campos
        composeTestRule.onNodeWithText("preencher todos os campos", ignoreCase = true, substring = true).assertIsDisplayed()
    }
}
