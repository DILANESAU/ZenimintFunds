package com.fintechforge.zenimintfunds.ui.cards

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel
import com.fintechforge.zenimintfunds.data.CompraMSI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MsiScreen(viewModel: FinanceViewModel, navController: NavController, tarjetaId: Int) {
    val compras by viewModel.obtenerComprasPorTarjeta(tarjetaId).collectAsState(initial = emptyList())
    var showPaymentConfirmDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var compraAEditar by remember { mutableStateOf<CompraMSI?>(null) }

    // --- CÁLCULOS ---
    val pagoTotalAlBanco = compras.sumOf { it.montoTotalOriginal / it.cuotasTotales }

    val miDeudaMensual = compras.filter { it.deudor == "Yo" }
        .sumOf { it.montoTotalOriginal / it.cuotasTotales }

    val cobranzaMensual = compras.filter { it.deudor != "Yo" }
        .groupBy { it.deudor }
        .mapValues { entry ->
            entry.value.sumOf { it.montoTotalOriginal / it.cuotasTotales }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control de MSI") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Compra")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            // 1. TARJETA DE RESUMEN (BANCO vs YO)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pago Total al Banco", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "$${String.format("%,.2f", pagoTotalAlBanco)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tu parte es: $${String.format("%,.2f", miDeudaMensual)}", style = MaterialTheme.typography.bodyMedium)
                }
                if (pagoTotalAlBanco > 0) {
                    Button(
                        onClick = { showPaymentConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Verde éxito
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrar Pago del Mes")
                    }
                }
            }

            // 2. TARJETAS DE COBRANZA (Si hay)
            if (cobranzaMensual.isNotEmpty()) {
                Text("Recuperar este mes:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cobranzaMensual.forEach { (nombre, monto) ->
                        AssistChip(
                            onClick = {},
                            label = { Text("$nombre: $${String.format("%,.0f", monto)}") },
                            leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // 3. LISTA DE COMPRAS
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(compras) { compra ->
                    MsiItem(
                        compra = compra,
                        onEdit = { compraAEditar = compra }, // Aquí activamos la edición
                        onDelete = { viewModel.borrarCompra(compra) }
                    )
                }
            }
        }

        // --- MANEJO DE DIÁLOGOS ---

        // CASO 1: AGREGAR NUEVA
        if (showAddDialog) {
            MsiFormDialog(
                titulo = "Nueva Compra",
                onDismiss = { showAddDialog = false },
                onConfirm = { desc, total, cuotas, deudor ->
                    viewModel.agregarCompraMSI(tarjetaId, desc, total, cuotas, deudor)
                    showAddDialog = false
                }
            )
        }

        // CASO 2: EDITAR EXISTENTE
        if (compraAEditar != null) {
            MsiFormDialog(
                titulo = "Editar Compra",
                compraInicial = compraAEditar, // Pasamos los datos actuales
                onDismiss = { compraAEditar = null },
                onConfirm = { desc, total, cuotas, deudor ->
                    val compraActualizada = compraAEditar!!.copy(
                        descripcion = desc,
                        montoTotalOriginal = total,
                        cuotasTotales = cuotas,
                        deudor = deudor
                    )
                    viewModel.actualizarCompra(compraActualizada)
                    compraAEditar = null
                }
            )
        }
        if (showPaymentConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showPaymentConfirmDialog = false },
                title = { Text("¿Confirmar Pago del Mes?") },
                text = {
                    Text("Esto sumará 1 pago a todas tus compras activas de esta tarjeta.\n\nTotal a procesar: $${String.format("%,.2f", pagoTotalAlBanco)}")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.pagarMensualidadTarjeta(tarjetaId, crearGastoEnDashboard = true)
                            showPaymentConfirmDialog = false
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Confirmar y Registrar Gasto")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentConfirmDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

// --- EL ÍTEM ÚNICO Y CORRECTO ---
@Composable
fun MsiItem(
    compra: CompraMSI,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progreso = if (compra.cuotasTotales > 0) compra.cuotasPagadas.toFloat() / compra.cuotasTotales else 0f
    val esMio = compra.deudor == "Yo"
    val colorFondo = if (esMio) MaterialTheme.colorScheme.surface else Color(0xFFFFF3E0)

    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = colorFondo),
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { showMenu = true }) // Detectar toque largo
            }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(compra.descripcion, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (!esMio) {
                            Text(
                                "Deudor: ${compra.deudor}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFEF6C00),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text("$${String.format("%,.0f", compra.montoTotalOriginal)}", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                        strokeCap = StrokeCap.Round,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${compra.cuotasPagadas}/${compra.cuotasTotales}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Editar") },
                onClick = { showMenu = false; onEdit() },
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )
            DropdownMenuItem(
                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

// --- DIÁLOGO REUTILIZABLE (SIRVE PARA AGREGAR Y EDITAR) ---
@Composable
fun MsiFormDialog(
    titulo: String,
    compraInicial: CompraMSI? = null, // Opcional: Si viene nulo es agregar, si viene con datos es editar
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String) -> Unit
) {
    // Si hay compraInicial, llenamos los campos con esos datos. Si no, vacíos.
    var desc by remember { mutableStateOf(compraInicial?.descripcion ?: "") }
    var total by remember { mutableStateOf(compraInicial?.montoTotalOriginal?.toString() ?: "") }
    var cuotas by remember { mutableStateOf(compraInicial?.cuotasTotales?.toString() ?: "") }
    var deudor by remember { mutableStateOf(if (compraInicial?.deudor == "Yo") "" else compraInicial?.deudor ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Producto (Ej: TV)") }
                )
                OutlinedTextField(
                    value = total,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) total = it },
                    label = { Text("Monto Total") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = cuotas,
                    onValueChange = { if (it.all { c -> c.isDigit() }) cuotas = it },
                    label = { Text("Plazo (Meses)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = deudor,
                    onValueChange = { deudor = it },
                    label = { Text("¿Quién paga? (Vacío = Yo)") },
                    placeholder = { Text("Ej: Juan, Mamá...") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (desc.isNotEmpty() && total.isNotEmpty() && cuotas.isNotEmpty()) {
                    val quienPaga = deudor.ifBlank { "Yo" }
                    onConfirm(desc, total.toDouble(), cuotas.toInt(), quienPaga)
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}