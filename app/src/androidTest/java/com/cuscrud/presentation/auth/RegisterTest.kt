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
class RegisterTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        // Navega para a tela de cadastro
        composeTestRule.onNodeWithText("Não tem uma conta? Cadastre-se").performClick()
    }

    @Test
    fun cadastroSucesso_RedirecionaParaLogin() {
        composeTestRule.onNodeWithText("Nome Completo").performTextInput("Novo Usuário")
        composeTestRule.onNodeWithText("E-mail").performTextInput("novo@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("12345678")

        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("CusCRUD").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("CusCRUD").assertIsDisplayed()
        composeTestRule.onNodeWithText("ENTRAR").assertIsDisplayed()
    }

    @Test
    fun cadastroEmailInvalido_InformaErroFormato() {
        // Quando o usuário insere um e-mail com formato inválido
        composeTestRule.onNodeWithText("Nome Completo").performTextInput("Teste")
        composeTestRule.onNodeWithText("E-mail").performTextInput("email_sem_arroba")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("12345678")

        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        // Então o sistema informa o erro de formato
        composeTestRule.onNodeWithText("E-mail com formato inválido. Use o padrão exemplo@dominio.com").assertIsDisplayed()
    }

    @Test
    fun cadastroSenhaCurta_InformaErroTamanho() {
        // Quando o usuário insere uma senha com menos de 8 caracteres
        composeTestRule.onNodeWithText("Nome Completo").performTextInput("Teste")
        composeTestRule.onNodeWithText("E-mail").performTextInput("teste@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("123")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("123")

        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        // Então o sistema informa o erro de limite
        composeTestRule.onNodeWithText("A senha deve ter entre 8 e 50 caracteres").assertIsDisplayed()
    }

    @Test
    fun cadastroNomeMuitoLongo_InformaErroLimite() {
        // Quando o usuário insere um nome com mais de 255 caracteres
        val nomeLongo = "a".repeat(256)
        composeTestRule.onNodeWithText("Nome Completo").performTextInput(nomeLongo)
        composeTestRule.onNodeWithText("E-mail").performTextInput("teste@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("12345678")

        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        composeTestRule.onNodeWithText("O nome deve ter no máximo 255 caracteres").assertIsDisplayed()
    }

    @Test
    fun cadastroSenhasNaoConferem_InformaErro() {
        composeTestRule.onNodeWithText("Nome Completo").performTextInput("Teste")
        composeTestRule.onNodeWithText("E-mail").performTextInput("teste@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("65432100")

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
        composeTestRule.onNodeWithText("E-mail").performTextInput("teste@email.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("12345678")

        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        composeTestRule.onNodeWithText("Já existe uma conta associada a este e-mail").assertIsDisplayed()
    }

    @Test
    fun cadastroFalhaConexao_ExibeMensagemErroServidor() {
        composeTestRule.onNodeWithText("Nome Completo").performTextInput("Erro Rede")
        composeTestRule.onNodeWithText("E-mail").performTextInput("timeout@teste.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("12345678")
        composeTestRule.onNodeWithText("Confirmar Senha").performTextInput("12345678")
        
        composeTestRule.onNodeWithText("CADASTRAR").performClick()

        composeTestRule.onNodeWithText("Não foi possível comunicar com o servidor. Tente novamente mais tarde.").assertIsDisplayed()
    }
}
