package com.cuscrud.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cuscrud.MainActivity
import com.cuscrud.presentation.detalhes.ProdutoDetalhesScreen
import com.cuscrud.presentation.detalhes.ProdutoDetalhesViewModel
import com.cuscrud.presentation.inventario.InventarioScreen
import com.cuscrud.presentation.inventario.InventarioViewModel
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
        startDestination = "inventario"
    ) {
        // Cenário 1: Inventário Geral
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
                onAddSampleData = { mainActivity.viewModel.addSampleProduct() }
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
                onBackClick = { success ->
                    if (success) {
                        // Se foi editado, volta para a tela inicial do inventário conforme Gherkin
                        navController.popBackStack("inventario", inclusive = false)
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
