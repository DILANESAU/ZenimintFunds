package com.fintechforge.zenimintfunds

import android.os.Bundle
import android.Manifest
import android.os.Build
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fintechforge.zenimintfunds.data.AppDatabase
import com.fintechforge.zenimintfunds.ui.income.AddIncomeScreen
import com.fintechforge.zenimintfunds.ui.expenses.AddExpenseScreen
import com.fintechforge.zenimintfunds.ui.dashboard.DashboardScreen
import com.fintechforge.zenimintfunds.ui.cards.AddCardScreen
import com.fintechforge.zenimintfunds.ui.cards.CardsScreen
import com.fintechforge.zenimintfunds.ui.cards.MsiScreen
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido
        } else {
            // Permiso denegado
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializar Base de Datos
        val database = AppDatabase.getDatabase(this)
        val dao = database.financeDao()

        // 2. Inicializar ViewModel
        val viewModel = FinanceViewModelFactory(dao).create(FinanceViewModel::class.java)

        iniciarWorker()
        // 3. PEDIR PERMISO DE NOTIFICACIÓN
        pedirPermisoNotificacion()

        setContent {
            // Asegúrate de usar tu tema si tienes uno, o MaterialTheme por defecto
            MaterialTheme {
                MainAppStructure(viewModel)
            }
        }
    }
    private fun iniciarWorker() {
        val workRequest = PeriodicWorkRequestBuilder<com.fintechforge.zenimintfunds.workers.FinanceWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FinanceCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun pedirPermisoNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun MainAppStructure(viewModel: FinanceViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Botón Inicio
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Inicio") },
                    selected = currentRoute == "home",
                    onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true } // Limpia la pila para no acumular pantallas
                        }
                    }
                )

                // Botón Tarjetas
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    label = { Text("Billetera") },
                    selected = currentRoute?.startsWith("cards") == true, // Se queda activo si estás en sub-pantallas
                    onClick = {
                        navController.navigate("cards") {
                            launchSingleTop = true // Evita abrir la misma pantalla mil veces
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        // --- AQUÍ ESTÁ EL MAPA DE NAVEGACIÓN COMPLETO ---
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Pantalla Principal
            composable("home") {
                DashboardScreen(viewModel, navController)
            }

            // 2. Agregar Gasto
            composable("add_expense") {
                AddExpenseScreen(viewModel, navController)
            }

            // 3. Lista de Tarjetas (Billetera)
            composable("cards") {
                CardsScreen(viewModel, navController)
            }

            composable("add_income") {
                AddIncomeScreen(viewModel, navController)
            }
            // 4. Agregar Nueva Tarjeta (¡Esta faltaba!)
            composable("add_card") {
                AddCardScreen(viewModel, navController)
            }

            // 5. Detalle de MSI (¡Esta es la clave!)
            // Definimos que esta ruta RECIBE un argumento llamado "tarjetaId"
            composable(
                route = "msi_details/{tarjetaId}",
                arguments = listOf(
                    navArgument("tarjetaId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                // Recuperamos el ID que enviamos desde la tarjeta
                val id = backStackEntry.arguments?.getInt("tarjetaId") ?: 0

                // Abrimos la pantalla pasando ese ID
                MsiScreen(viewModel, navController, id)
            }
        }
    }
}