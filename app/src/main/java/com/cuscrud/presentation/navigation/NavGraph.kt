package com.cuscrud.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cuscrud.MainActivity
import com.cuscrud.presentation.auth.LoginScreen
import com.cuscrud.presentation.auth.LoginViewModel
import com.cuscrud.presentation.auth.RegisterScreen
import com.cuscrud.presentation.auth.RegisterViewModel
import com.cuscrud.presentation.detalhes.ProdutoDetalhesScreen
import com.cuscrud.presentation.detalhes.ProdutoDetalhesViewModel
import com.cuscrud.presentation.inventario.InventarioScreen
import com.cuscrud.presentation.inventario.InventarioViewModel
import com.cuscrud.presentation.ong.CreateOngScreen
import com.cuscrud.presentation.ong.CreateOngViewModel
import com.cuscrud.presentation.ong.SelectOngScreen
import com.cuscrud.presentation.ong.SelectOngViewModel
import com.cuscrud.presentation.ong.OngSettingsScreen
import com.cuscrud.presentation.ong.OngSettingsViewModel
import com.cuscrud.presentation.produtos.AddProdutoScreen
import com.cuscrud.presentation.produtos.AddProdutoViewModel
import com.cuscrud.presentation.produtos.ProdutosPorTipoScreen
import com.cuscrud.presentation.produtos.ProdutosPorTipoViewModel

@Composable
fun CusCrudNavGraph(
    navController: NavHostController,
    mainActivity: MainActivity
) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        // Cenário: Login
        composable("login") {
            val viewModel = hiltViewModel<LoginViewModel>()
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate("select_ong") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate("register")
                }
            )
        }

        // Cenário: Registro
        composable("register") {
            val viewModel = hiltViewModel<RegisterViewModel>()
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Cenário: Seleção de ONG (Contexto ativo)
        composable("select_ong") {
            val viewModel = hiltViewModel<SelectOngViewModel>()
            SelectOngScreen(
                viewModel = viewModel,
                onOngSelected = {
                    navController.navigate("inventario") {
                        popUpTo("select_ong") { inclusive = true }
                    }
                },
                onCreateOngClick = {
                    navController.navigate("create_ong")
                }
            )
        }

        // Cenário: Criação de ONG
        composable("create_ong") {
            val viewModel = hiltViewModel<CreateOngViewModel>()
            CreateOngScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onOngCreated = {
                    navController.navigate("inventario") {
                        popUpTo("select_ong") { inclusive = true }
                    }
                }
            )
        }

        // Cenário: Definições da ONG
        composable("ong_settings") {
            val viewModel = hiltViewModel<OngSettingsViewModel>()
            OngSettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onDeleteSuccess = {
                    navController.navigate("select_ong") {
                        popUpTo("inventario") { inclusive = true }
                    }
                }
            )
        }

        // Cenário 1: Inventário Geral (da ONG selecionada)
        composable("inventario") {
            val viewModel = hiltViewModel<InventarioViewModel>()
            InventarioScreen(
                viewModel = viewModel,
                navController = navController,
                onTipoSelected = { tipoId ->
                    navController.navigate("produtos/$tipoId")
                },
                onAddProdutoClick = {
                    navController.navigate("add_produto")
                },
                onAddSampleData = { mainActivity.viewModel.addSampleProduct() },
                onChangeOngClick = {
                    navController.navigate("select_ong")
                },
                onSettingsClick = {
                    navController.navigate("ong_settings")
                }
            )
        }

        // Cenário 2: Produtos por Tipo
        composable(
            route = "produtos/{tipoId}",
            arguments = listOf(navArgument("tipoId") { type = NavType.LongType })
        ) {
            val tipoId = it.arguments?.getLong("tipoId") ?: 0L
            val viewModel = hiltViewModel<ProdutosPorTipoViewModel>()
            ProdutosPorTipoScreen(
                viewModel = viewModel,
                navController = navController,
                onBackClick = { navController.popBackStack() },
                onProdutoClick = { produtoId ->
                    navController.navigate("detalhes/$produtoId")
                },
                onAddProdutoClick = { navController.navigate("add_produto?tipoId=$tipoId") }
            )
        }

        // Cenário para Adicionar ou Editar Produto
        composable(
            route = "add_produto?tipoId={tipoId}&produtoId={produtoId}",
            arguments = listOf(
                navArgument("tipoId") { 
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("produtoId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            val viewModel = hiltViewModel<AddProdutoViewModel>()
            AddProdutoScreen(
                viewModel = viewModel,
                onBackClick = { message ->
                    if (message != null) {
                        if (message.contains("editado", ignoreCase = true)) {
                            navController.getBackStackEntry("inventario")
                                .savedStateHandle
                                .set("success_message", message)
                            navController.popBackStack("inventario", inclusive = false)
                        } else {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("success_message", message)
                            navController.popBackStack()
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        // Cenário 3: Detalhes do Produto
        composable(
            route = "detalhes/{produtoId}",
            arguments = listOf(navArgument("produtoId") { type = NavType.IntType })
        ) {
            val viewModel = hiltViewModel<ProdutoDetalhesViewModel>()
            ProdutoDetalhesScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onEditClick = { produtoId ->
                    navController.navigate("add_produto?produtoId=$produtoId")
                }
            )
        }
    }
}
