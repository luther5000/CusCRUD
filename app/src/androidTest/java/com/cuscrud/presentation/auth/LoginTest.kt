package com.cuscrud.presentation.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import com.cuscrud.MainActivity
import com.cuscrud.domain.repository.AuthRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class LoginTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        hiltRule.inject()
        // Garante que o usuário esteja deslogado antes de cada teste
        // para evitar side effects de redirecionamento automático.
        runBlocking {
            authRepository.logout()
        }
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
        // Quando o usuário insere um email válido (formato) mas senha incorreta para o servidor
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("existente@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senha_errada_longa")
        
        Espresso.closeSoftKeyboard()

        // E clica no botão de entrar
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema exibe a mensagem de erro vinda do repositório/servidor
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("E-mail ou senha incorretos", ignoreCase = true, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun loginCamposEmBranco_InformaPreenchimentoObrigatorio() {
        // Quando o usuário deixa o campo de email ou senha em branco
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema informa que é necessário preencher todos os campos
        composeTestRule.onNodeWithText("É necessário preencher todos os campos", ignoreCase = true, substring = true).assertIsDisplayed()
    }

    @Test
    fun loginEmailFormatoInvalido_InformaErro() {
        // Quando o usuário insere um e-mail com formato inválido
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("email_invalido")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema informa erro de formato de e-mail
        composeTestRule.onNodeWithText("E-mail com formato inválido. Use o padrão exemplo@dominio.com").assertIsDisplayed()
    }

    @Test
    fun loginSenhaCurta_InformaErroTamanho() {
        // Quando o usuário insere uma senha com menos de 8 caracteres
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput("teste@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("123")
        
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema informa que a senha deve ter pelo menos 8 caracteres
        composeTestRule.onNodeWithText("A senha deve ter entre 8 e 50 caracteres").assertIsDisplayed()
    }

    @Test
    fun loginEmailMuitoLongo_InformaErroLimite() {
        // Quando o usuário insere um e-mail com mais de 255 caracteres
        val emailLongo = "a".repeat(246) + "@teste.com" // Total 256
        composeTestRule.onNodeWithText("E-mail ou Login").performTextInput(emailLongo)
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("ENTRAR").performClick()

        // Então o sistema informa o erro de limite de caracteres
        composeTestRule.onNodeWithText("O e-mail deve ter no máximo 255 caracteres").assertIsDisplayed()
    }
}
