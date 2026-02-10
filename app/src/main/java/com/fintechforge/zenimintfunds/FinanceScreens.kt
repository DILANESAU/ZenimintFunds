package com.fintechforge.zenimintfunds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.FinanceViewModel
import com.fintechforge.zenimintfunds.data.GastoDiario
import java.text.SimpleDateFormat
import java.util.*

// --- PANTALLA PRINCIPAL (DASHBOARD) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FinanceViewModel, navController: NavController) {
    // Obtenemos los datos en tiempo real
    val gastos by viewModel.gastos.collectAsState()

    // Calculamos el total gastado (luego lo haremos más avanzado por mes)
    val totalGastado = gastos.sumOf { it.monto }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis Finanzas", style = MaterialTheme.typography.titleMedium)
                        Text("Resumen General", style = MaterialTheme.typography.bodySmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_expense") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Gasto")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. TARJETA DE RESUMEN (Estilo degradado o sólido elegante)
            BalanceCard(total = totalGastado)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. TÍTULO DE SECCIÓN
            Text(
                text = "Últimos Movimientos",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
                fontWeight = FontWeight.Bold
            )

            // 3. LISTA DE GASTOS
            if (gastos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay gastos registrados aún", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gastos) { gasto ->
                        GastoItem(gasto)
                    }
                }
            }
        }
    }
}

// COMPONENTE: Tarjeta de Balance
@Composable
fun BalanceCard(total: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(150.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Gasto Total", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "$${String.format("%.2f", total)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// COMPONENTE: Fila de Gasto Individual
@Composable
fun GastoItem(gasto: GastoDiario) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    ListItem(
        headlineContent = { Text(gasto.descripcion, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(gasto.categoria) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-$${gasto.monto}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(Date(gasto.fechaGasto)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.ShoppingBag, // Podríamos cambiar ícono según categoría
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    )
}

// --- PANTALLA PARA AGREGAR GASTO ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(viewModel: FinanceViewModel, navController: NavController) {
    var monto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("General") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Gasto") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo Monto
            OutlinedTextField(
                value = monto,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) monto = it },
                label = { Text("Monto ($)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            // Campo Descripción
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción (ej: Tacos)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Botón Guardar
            Button(
                onClick = {
                    if (monto.isNotEmpty() && descripcion.isNotEmpty()) {
                        viewModel.agregarGasto(monto.toDouble(), descripcion, categoria)
                        navController.popBackStack() // Vuelve atrás al guardar
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar Gasto")
            }
        }
    }
}