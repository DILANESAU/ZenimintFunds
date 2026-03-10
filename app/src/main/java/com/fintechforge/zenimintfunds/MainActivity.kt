package com.fintechforge.zenimintfunds

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
// IMPORTANTE: Cambiamos ComponentActivity por FragmentActivity para la huella
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fintechforge.zenimintfunds.data.AppDatabase
import com.fintechforge.zenimintfunds.ui.cards.AddCardScreen
import com.fintechforge.zenimintfunds.ui.cards.CardsScreen
import com.fintechforge.zenimintfunds.ui.cards.MsiScreen
import com.fintechforge.zenimintfunds.ui.dashboard.DashboardScreen
import com.fintechforge.zenimintfunds.ui.expenses.AddExpenseScreen
import com.fintechforge.zenimintfunds.ui.income.AddIncomeScreen
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModelFactory
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.fintechforge.zenimintfunds.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

// 1. CAMBIO CLAVE: Heredamos de FragmentActivity
class MainActivity : FragmentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permiso de notificaciones
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val dao = database.financeDao()
        val viewModel = FinanceViewModelFactory(dao).create(FinanceViewModel::class.java)

        iniciarWorker()
        programarRecordatorioNocturno()
        pedirPermisoNotificacion()

        setContent {
            com.fintechforge.zenimintfunds.ui.theme.ZenimintFundsTheme {

                // 2. ESTADO DE SEGURIDAD (Por defecto, bloqueado)
                var isUnlocked by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isUnlocked) {
                        // Si está desbloqueado, mostramos la app normal
                        MainAppStructure(viewModel)
                    } else {
                        // Si está bloqueado, mostramos la pantalla de seguridad
                        LockedScreen(
                            onUnlockClick = {
                                showBiometricPrompt { success ->
                                    isUnlocked = success // Si la huella es correcta, se desbloqueea
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // --- FUNCIÓN QUE LLAMA AL LECTOR DE HUELLA NATIVO ---
    private fun showBiometricPrompt(onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // ¡Huella correcta!
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // El usuario canceló o hubo error
                    onResult(false)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Huella no reconocida
                    onResult(false)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Zenimint")
            .setSubtitle("Accede a tu bóveda financiera")
            .setNegativeButtonText("Cancelar")
            // .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG) // Opcional
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // ... (Mantén tus funciones iniciarWorker y pedirPermisoNotificacion igual) ...
    private fun iniciarWorker() {
        val workRequest = PeriodicWorkRequestBuilder<com.fintechforge.zenimintfunds.workers.FinanceWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("FinanceCheck", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }

    private fun pedirPermisoNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    private fun programarRecordatorioNocturno() {
        val currentDate = java.util.Calendar.getInstance()
        val dueDate = java.util.Calendar.getInstance()

        // Configuramos a las 9:00 PM (21:00 hrs)
        dueDate.set(java.util.Calendar.HOUR_OF_DAY, 21)
        dueDate.set(java.util.Calendar.MINUTE, 0)
        dueDate.set(java.util.Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(java.util.Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.fintechforge.zenimintfunds.workers.ReminderWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        )
            .setInitialDelay(timeDiff, java.util.concurrent.TimeUnit.MILLISECONDS)
            .addTag("DailyReminder")
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyReminder",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}

// --- PANTALLA DE BLOQUEO ELEGANTE ---
@Composable
fun LockedScreen(onUnlockClick: () -> Unit) {
    // Al abrir la app, intentamos pedir la huella automáticamente
    LaunchedEffect(Unit) {
        onUnlockClick()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Bloqueado",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Zenimint Funds", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Bóveda Segura", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onUnlockClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Desbloquear con Biometría", fontSize = 16.sp)
        }
    }
}

// ... (Acá abajo mantén tu MainAppStructure exactamente igual a como lo tenías) ...
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainAppStructure(viewModel: FinanceViewModel) {
    // El NavController ahora solo gestiona "capas por encima" (como agregar gastos)
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main_tabs" // Arrancamos en nuestro contenedor deslizable
    ) {
        // 1. EL CONTENEDOR PRINCIPAL DESLIZABLE (Dashboard + Billetera)
        composable("main_tabs") { MainTabsScreen(viewModel, navController) }

        // 2. PANTALLAS SECUNDARIAS (Se abren "encima" del menú)
        composable("add_expense") { AddExpenseScreen(viewModel, navController) }
        composable("add_income") { AddIncomeScreen(viewModel, navController) }
        composable("add_card") { AddCardScreen(viewModel, navController) }
        composable("settings") { SettingsScreen(viewModel, navController) }
        composable(
            "msi_details/{tarjetaId}",
            arguments = listOf(navArgument("tarjetaId") { type = NavType.IntType })
        ) {
            val id = it.arguments?.getInt("tarjetaId") ?: 0
            MsiScreen(viewModel, navController, id)
        }
    }
}

// --- EL MOTOR DE DESLIZAMIENTO INFERIOR ---
@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun MainTabsScreen(viewModel: FinanceViewModel, navController: androidx.navigation.NavController) {
    // Creamos un pager con 2 páginas
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = "Billetera") },
                    label = { Text("Billetera") },
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    }
                )
            }
        }
    ) { innerPadding ->
        // Aquí conectamos el deslizamiento con las pantallas reales
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) { page ->
            when (page) {
                0 -> DashboardScreen(viewModel, navController) // Página 0 = Dashboard
                1 -> CardsScreen(viewModel, navController)     // Página 1 = Billetera
            }
        }
    }
}
