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
class RegisterTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Garante estado limpo antes de iniciar o teste de cadastro
        runBlocking {
            authRepository.logout()
        }

        // Navega para a tela de cadastro
        composeTestRule.onNodeWithText("Não tem uma conta? Cadastre-se").performClick()
    }

    @Test
    fun cadastroSucesso_RedirecionaParaLogin() {
        composeTestRule.onNodeWithText("Nome Completo").performTextInput("Novo Usuário")
        composeTestRule.onNodeWithText("E-mail").performTextInput("novo@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("12345678")

        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("CusCRUD").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("CusCRUD").assertIsDisplayed()
        composeTestRule.onNodeWithText("ENTRAR").assertIsDisplayed()
    }

    @Test
    fun cadastroEmailInvalido_InformaErroFormato() {
        composeTestRule.onNodeWithText("Nome Completo").performTextInput("Teste")
        composeTestRule.onNodeWithText("E-mail").performTextInput("email_sem_arroba")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("12345678")

        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        composeTestRule.onNodeWithText("E-mail com formato inválido. Use o padrão exemplo@dominio.com").assertIsDisplayed()
    }

    @Test
    fun cadastroSenhaCurta_InformaErroTamanho() {
        composeTestRule.onNodeWithText("Nome Completo").performTextInput("Teste")
        composeTestRule.onNodeWithText("E-mail").performTextInput("teste@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("123")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("123")

        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        composeTestRule.onNodeWithText("A senha deve ter entre 8 e 50 caracteres").assertIsDisplayed()
    }

    @Test
    fun cadastroNomeMuitoLongo_InformaErroLimite() {
        val nomeLongo = "a".repeat(256)
        composeTestRule.onNodeWithText("Nome Completo").performTextInput(nomeLongo)
        composeTestRule.onNodeWithText("E-mail").performTextInput("teste@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("12345678")

        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        composeTestRule.onNodeWithText("O nome deve ter no máximo 255 caracteres").assertIsDisplayed()
    }

    @Test
    fun cadastroSenhasNaoConferem_InformaErro() {
        composeTestRule.onNodeWithText("Nome Completo").performTextInput("Teste")
        composeTestRule.onNodeWithText("E-mail").performTextInput("teste@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("65432100")

        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        composeTestRule.onNodeWithText("As senhas não coincidem").assertIsDisplayed()
    }

    @Test
    fun cadastroCamposVazios_InformaPreenchimentoObrigatorio() {
        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        composeTestRule.onNodeWithText("É necessário preencher todos os campos obrigatórios").assertIsDisplayed()
    }

    @Test
    fun cadastroEmailExistente_InformaErro() {
        composeTestRule.onNodeWithText("Nome Completo").performTextInput("Usuario Existente")
        composeTestRule.onNodeWithText("E-mail").performTextInput("joao.novo@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("12345678")

        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        // Aguarda a resposta do servidor e a exibição da Snackbar
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("já está em uso", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun cadastroFalhaConexao_ExibeMensagemErroServidor() {
        composeTestRule.onNodeWithText("Nome Completo").performTextInput("Erro Rede")
        composeTestRule.onNodeWithText("E-mail").performTextInput("timeout@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("12345678")
        
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        // Aguarda a resposta de erro genérico do servidor
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Não foi possível se conectar ao servidor.", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
