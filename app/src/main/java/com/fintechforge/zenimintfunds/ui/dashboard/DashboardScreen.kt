package com.fintechforge.zenimintfunds.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel
import com.fintechforge.zenimintfunds.data.GastoDiario
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FinanceViewModel, navController: NavController) {
    val gastos by viewModel.gastosFiltrados.collectAsState()
    val ingresos by viewModel.ingresos.collectAsState()
    val fechaActual by viewModel.fechaActual.collectAsState()
    val categoriasGasto by viewModel.categoriasGasto.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val totalGastado = gastos.sumOf { it.monto }
    val totalIngresado = ingresos.sumOf { it.monto }
    val balance = totalIngresado - totalGastado

    var gastoAEditar by remember { mutableStateOf<GastoDiario?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Hola, Usuario", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate("settings")}) {
                        Box(
                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("U", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // 1. SELECTOR DE MES
            item {
                MonthSelector(
                    fechaActual = fechaActual,
                    onMesAnterior = { viewModel.cambiarMes(-1) },
                    onMesSiguiente = { viewModel.cambiarMes(1) }
                )
            }

            // 2. HERO BALANCE
            item { HeroBalanceSection(balance = balance) }

            // 3. ACCIONES RÁPIDAS
            item {
                QuickActionsRow(
                    onAddExpense = { navController.navigate("add_expense") },
                    onAddIncome = { navController.navigate("add_income") },
                    onAddCard = { navController.navigate("add_card") }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 4. GRÁFICO DE DONA (¡NUEVO!)
            if (gastos.isNotEmpty()) {
                item {
                    Text(
                        "Análisis de Gastos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    DonutChartSection(gastos = gastos, categoriasBD = categoriasGasto)
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // 5. CABECERA DE LISTA DE MOVIMIENTOS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            // 6. LISTA DE GASTOS
            if (gastos.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Todo zen por aquí", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(gastos) { gasto ->
                    ModernTransactionItem(
                        gasto = gasto,
                        onClick = {
                            gastoAEditar = gasto
                            showEditDialog = true
                        }
                    )
                    Divider(modifier = Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }

    // Diálogo de Edición
    if (showEditDialog && gastoAEditar != null) {
        EditExpenseDialog(
            gasto = gastoAEditar!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { m, d ->
                viewModel.actualizarGasto(gastoAEditar!!.copy(monto = m, descripcion = d))
                showEditDialog = false
            }
        )
    }
}

// --- EL MOTOR DEL GRÁFICO (NUEVO) ---

@Composable
fun DonutChartSection(gastos: List<GastoDiario>, categoriasBD: List<com.fintechforge.zenimintfunds.data.Categoria>) {
    // 1. Agrupar gastos por categoría y sumar montos
    val categoryTotals = gastos.groupBy { it.categoria }
        .mapValues { it.value.sumOf { gasto -> gasto.monto } }
        .toList()
        .sortedByDescending { it.second }

    val totalGasto = categoryTotals.sumOf { it.second }

    // Función rápida para encontrar el color de una categoría
    fun getColorParaCategoria(nombreCat: String): Color {
        val colorHex = categoriasBD.find { it.nombre == nombreCat }?.colorHex
        return if (colorHex != null) Color(colorHex) else Color.Gray // Gris por si no lo encuentra
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // EL CÍRCULO
        Box(modifier = Modifier.size(200.dp).padding(16.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f

                categoryTotals.forEach { (nombreCategoria, amount) ->
                    val sweepAngle = ((amount / totalGasto) * 360f).toFloat()
                    val catColor = getColorParaCategoria(nombreCategoria)

                    drawArc(
                        color = catColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 60f, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }
            // Texto en el centro de la dona
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Gastado", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("-$${String.format("%,.0f", totalGasto)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LA LEYENDA
        Column(modifier = Modifier.fillMaxWidth()) {
            categoryTotals.forEach { (categoria, monto) ->
                val porcentaje = (monto / totalGasto) * 100
                val catColor = getColorParaCategoria(categoria)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(catColor, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(categoria, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$${String.format("%,.0f", monto)}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("${String.format("%.1f", porcentaje)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// --- COMPONENTES AUXILIARES (Los que ya teníamos) ---

@Composable
fun HeroBalanceSection(balance: Double) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Balance Total", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$${String.format("%,.2f", balance)}",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun QuickActionsRow(onAddExpense: () -> Unit, onAddIncome: () -> Unit, onAddCard: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        QuickActionButton(icon = Icons.Default.ArrowDownward, text = "Gasto", color = Color(0xFFFF8A80), onClick = onAddExpense)
        QuickActionButton(icon = Icons.Default.ArrowUpward, text = "Ingreso", color = Color(0xFF69F0AE), onClick = onAddIncome)
        QuickActionButton(icon = Icons.Default.CreditCard, text = "Tarjeta", color = MaterialTheme.colorScheme.primary, onClick = onAddCard)
    }
}

@Composable
fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(60.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)).clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = text, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ModernTransactionItem(gasto: GastoDiario, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(gasto.categoria.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(gasto.descripcion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(gasto.categoria, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Text("-$${gasto.monto}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EditExpenseDialog(gasto: GastoDiario, onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var monto by remember { mutableStateOf(gasto.monto.toString()) }
    var descripcion by remember { mutableStateOf(gasto.descripcion) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Gasto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = monto, onValueChange = { monto = it }, label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") })
            }
        },
        confirmButton = { Button(onClick = { if (monto.isNotEmpty()) onConfirm(monto.toDouble(), descripcion) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}