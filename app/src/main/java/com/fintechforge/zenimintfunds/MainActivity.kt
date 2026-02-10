package com.fintechforge.zenimintfunds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fintechforge.zenimintfunds.data.AppDatabase
import com.fintechforge.zenimintfunds.DashboardScreen
import com.fintechforge.zenimintfunds.AddExpenseScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializamos la base de datos
        val database = AppDatabase.getDatabase(this)
        val dao = database.financeDao()

        // 2. Creamos el ViewModel manualmente (usando el Factory)
        val viewModel = FinanceViewModelFactory(dao).create(FinanceViewModel::class.java)

        setContent {
            // Usamos el tema por defecto de tu proyecto
            MaterialTheme {
                MainAppStructure(viewModel)
            }
        }
    }
}

@Composable
fun MainAppStructure(viewModel: FinanceViewModel) {
    val navController = rememberNavController()

    // Configuración del menú inferior
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Inicio") },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    label = { Text("Tarjetas") },
                    selected = currentRoute == "cards",
                    onClick = { /* Pendiente: Crear pantalla tarjetas */ }
                )
            }
        }
    ) { innerPadding ->
        // Sistema de Navegación
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                DashboardScreen(viewModel, navController)
            }
            composable("add_expense") {
                AddExpenseScreen(viewModel, navController)
            }
            // Aquí agregaremos luego la pantalla de tarjetas
            composable("cards") {
                Text("Pantalla de Tarjetas (Próximamente)")
            }
        }
    }
}