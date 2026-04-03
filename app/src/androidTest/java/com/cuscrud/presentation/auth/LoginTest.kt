package com.cuscrud.presentation.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        // (Assumindo que o app inicia na tela de login)

        // Quando ele insere seu email e senha corretos
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("admin")
        composeTestRule.onNodeWithText("Senha").performTextInput("123456")

        // E clica no botão de entrar
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema autentica o usuário e redireciona
        // Verificamos se algum elemento da tela 'select_ong' aparece (ex: título ou botão)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Selecionar Organização").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Criar Nova ONG").fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithText("Selecionar Organização").assertIsDisplayed()
    }

    @Test
    fun loginCredenciaisIncorretas_ExibeMensagemErro() {
        // Quando o usuário insere um email ou senha incorretos
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("errado@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senha_errada")

        // E clica no botão de entrar
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema exibe uma mensagem informando que as credenciais são inválidas
        // (A mensagem exata depende do retorno do Mock ou API, geralmente "Credenciais inválidas")
        composeTestRule.onNodeWithText("Credenciais inválidas").assertIsDisplayed()
        
        // E o usuário permanece na tela de login
        composeTestRule.onNodeWithText("ENTRAR").assertIsDisplayed()
    }

    @Test
    fun loginCamposEmBranco_InformaPreenchimentoObrigatorio() {
        // Quando o usuário deixa o campo de email ou senha em branco
        // (Clica direto no entrar)
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema informa que é necessário preencher todos os campos
        composeTestRule.onNodeWithText("Por favor, preencha todos os campos.").assertIsDisplayed()
    }

    @Test
    fun loginFalhaConexao_ExibeMensagemErroServidor() {
        // Simulação de erro de rede (pode exigir configuração de Mock no Repository)
        // Para este teste de UI, assumimos que o login falha por timeout ou 500
        
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("timeout@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("123456")
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema exibe a mensagem de falha de comunicação
        composeTestRule.onNodeWithText("Não foi possível comunicar com o servidor.").assertIsDisplayed()
    }
}
