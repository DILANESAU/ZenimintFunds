package com.fintechforge.zenimintfunds.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel
import com.fintechforge.zenimintfunds.data.GastoDiario
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FinanceViewModel, navController: NavController) {
    val gastos by viewModel.gastosFiltrados.collectAsState()
    val ingresos by viewModel.ingresos.collectAsState()
    val fechaActual by viewModel.fechaActual.collectAsState()

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
                title = {
                    Text(
                        "Hola, Usuario",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* Todo: Perfil */ }) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
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
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            item {
                MonthSelector(
                    fechaActual = fechaActual,
                    onMesAnterior = { viewModel.cambiarMes(-1) },
                    onMesSiguiente = { viewModel.cambiarMes(1) }
                )
            }

            item {
                HeroBalanceSection(balance = balance, onVisibilityToggle = {})
            }

            item {
                QuickActionsRow(
                    onAddExpense = { navController.navigate("add_expense") },
                    onAddIncome = { navController.navigate("add_income") },
                    onAddCard = { navController.navigate("add_card") }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tus Movimientos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { /* Ver todo */ }) {
                        Text("Ver todo")
                    }
                }
            }

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
                    Divider(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

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


@Composable
fun BalanceCard(balance: Double, ingresos: Double, gastos: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Balance Disponible",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${String.format("%,.2f", balance)}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ingresos", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text("+$${String.format("%,.0f", ingresos)}", color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold)
                    }

                    // Línea divisoria vertical
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha=0.3f)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Gastos", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text("-$${String.format("%,.0f", gastos)}", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GastoItem(gasto: GastoDiario, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { showMenu = true })
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono Circular
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Text(gasto.descripcion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(gasto.categoria, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            // Monto y Fecha
            Column(horizontalAlignment = Alignment.End) {
                Text("-$${gasto.monto}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Text(dateFormat.format(Date(gasto.fechaGasto)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Editar") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
            DropdownMenuItem(text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
        }
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
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (monto.isNotEmpty()) onConfirm(monto.toDouble(), descripcion) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}


@Composable
fun HeroBalanceSection(balance: Double, onVisibilityToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Balance Total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // El número GIGANTE
        Text(
            text = "$${String.format("%,.2f", balance)}",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun QuickActionsRow(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddCard: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        QuickActionButton(icon = Icons.Default.ArrowDownward, text = "Gastar", color = Color(0xFFFF8A80), onClick = onAddExpense)
        QuickActionButton(icon = Icons.Default.ArrowUpward, text = "Ingresar", color = Color(0xFF69F0AE), onClick = onAddIncome)
        QuickActionButton(icon = Icons.Default.CreditCard, text = "Tarjeta", color = MaterialTheme.colorScheme.primary, onClick = onAddCard)
    }
}

@Composable
fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ModernTransactionItem(gasto: GastoDiario, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = gasto.categoria.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gasto.descripcion,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = gasto.categoria,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Text(
            text = "-$${gasto.monto}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

