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
                onTipoSelected = { tipoId ->
                    navController.navigate("produtos/$tipoId")
                },
                onAddSampleData = { mainActivity.viewModel.addSampleProduct() }
            )
        }

        // Cenário 2: Produtos por Tipo
        composable(
            route = "produtos/{tipoId}",
            arguments = listOf(navArgument("tipoId") { type = NavType.LongType })
        ) {
            val viewModel = hiltViewModel<ProdutosPorTipoViewModel>()
            ProdutosPorTipoScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onProdutoClick = { produtoId ->
                    navController.navigate("detalhes/$produtoId")
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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}