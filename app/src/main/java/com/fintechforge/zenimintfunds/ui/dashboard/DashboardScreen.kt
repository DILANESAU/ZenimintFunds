package com.fintechforge.zenimintfunds.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel
import com.fintechforge.zenimintfunds.data.GastoDiario
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FinanceViewModel, navController: NavController) {
    // Escuchamos Gastos e Ingresos
    val gastos by viewModel.gastosFiltrados.collectAsState()
    val ingresos by viewModel.ingresos.collectAsState()

    val fechaActual by viewModel.fechaActual.collectAsState()
    var gastoAEditar by remember { mutableStateOf<GastoDiario?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val totalGastado = gastos.sumOf { it.monto }
    val totalIngresado = ingresos.sumOf { it.monto }
    val balance = totalIngresado - totalGastado

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Resumen Financiero") })
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallFloatingActionButton(
                    onClick = { navController.navigate("add_income") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Wallet, contentDescription = "Ingreso")
                }

                FloatingActionButton(
                    onClick = { navController.navigate("add_expense") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Gasto")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            MonthSelector(
                fechaActual = fechaActual,
                onMesAnterior = { viewModel.cambiarMes(-1) },
                onMesSiguiente = { viewModel.cambiarMes(1) }
            )

            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Balance Disponible", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "$${String.format("%,.2f", balance)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) Color.Black else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ResumenItem("Ingresos", totalIngresado, Color(0xFF2E7D32), Icons.Default.ArrowUpward)
                        ResumenItem("Gastos", totalGastado, MaterialTheme.colorScheme.error, Icons.Default.ArrowDownward)
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(gastos) { gasto ->
                            GastoItem(
                                gasto = gasto,
                                onDelete = { viewModel.borrarGasto(gasto) }, // ¡Borra directo!
                                onEdit = {
                                    gastoAEditar = gasto
                                    showEditDialog = true
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = "Últimos Movimientos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )


        }
        if (showEditDialog && gastoAEditar != null) {
            EditExpenseDialog(
                gasto = gastoAEditar!!,
                onDismiss = { showEditDialog = false },
                onConfirm = { nuevoMonto, nuevaDesc ->
                    // Creamos una copia del gasto con los datos nuevos
                    val gastoActualizado = gastoAEditar!!.copy(
                        monto = nuevoMonto,
                        descripcion = nuevaDesc
                    )
                    viewModel.actualizarGasto(gastoActualizado)
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
fun EditExpenseDialog(
    gasto: GastoDiario,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
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
            Button(onClick = {
                if (monto.isNotEmpty() && descripcion.isNotEmpty()) {
                    onConfirm(monto.toDouble(), descripcion)
                }
            }) { Text("Guardar Cambios") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
@Composable
fun ResumenItem(label: String, monto: Double, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("$${String.format("%,.0f", monto)}", fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun GastoItem(
    gasto: GastoDiario,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    var showMenu by remember { mutableStateOf(false) }

    Box {
        ListItem(
            headlineContent = { Text(gasto.descripcion, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(gasto.categoria) },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    Text("-$${gasto.monto}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Text(dateFormat.format(Date(gasto.fechaGasto)), style = MaterialTheme.typography.bodySmall)
                }
            },
            leadingContent = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showMenu = true },
                        onTap = { /* Opcional: Hacer algo al toque simple */ }
                    )
                }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Editar") },
                onClick = {
                    showMenu = false
                    onEdit()
                },
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )
            DropdownMenuItem(
                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}