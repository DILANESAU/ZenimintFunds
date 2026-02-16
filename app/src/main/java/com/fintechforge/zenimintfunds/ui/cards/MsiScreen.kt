package com.fintechforge.zenimintfunds.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel
import com.fintechforge.zenimintfunds.data.CompraMSI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MsiScreen(viewModel: FinanceViewModel, navController: NavController, tarjetaId: Int) {
    val compras by viewModel.obtenerComprasPorTarjeta(tarjetaId).collectAsState(initial = emptyList())
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showAddDialog by remember { mutableStateOf(false) }
    var compraAEditar by remember { mutableStateOf<CompraMSI?>(null) }
    var showPaymentConfirmDialog by remember { mutableStateOf(false) }

    val pagoTotalAlBanco = compras.sumOf { it.montoTotalOriginal / it.cuotasTotales }
    val miDeudaMensual = compras.filter { it.deudor == "Yo" }.sumOf { it.montoTotalOriginal / it.cuotasTotales }
    val cobranzaMensual = compras.filter { it.deudor != "Yo" }.groupBy { it.deudor }.mapValues { entry -> entry.value.sumOf { it.montoTotalOriginal / it.cuotasTotales } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Detalle Mensual") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape // FAB totalmente circular clásico y elegante
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Compra")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Pago para no generar intereses",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$${String.format("%,.2f", pagoTotalAlBanco)}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (miDeudaMensual < pagoTotalAlBanco) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Tu parte: $${String.format("%,.2f", miDeudaMensual)}") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            border = null
                        )
                    }
                }
            }

            if (pagoTotalAlBanco > 0) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Button(
                            onClick = { showPaymentConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Registrar Pago del Mes", fontSize = 16.sp)
                        }
                    }
                }
            }

            if (cobranzaMensual.isNotEmpty()) {
                item {
                    Text(
                        "Recuperar de terceros",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cobranzaMensual.forEach { (nombre, monto) ->
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text("$nombre: $${String.format("%,.0f", monto)}") },
                                leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            if (compras.isNotEmpty()) {
                item {
                    Text(
                        "Desglose",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                    )
                }
                items(compras) { compra ->
                    MsiMinimalItem(
                        compra = compra,
                        onEdit = { compraAEditar = compra },
                        onDelete = { viewModel.borrarCompra(compra) }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }


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

    if (compraAEditar != null) {
        MsiFormDialog(
            titulo = "Editar Compra",
            compraInicial = compraAEditar,
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
                        // Llamamos a la función mágica
                        viewModel.pagarMensualidadTarjeta(tarjetaId, crearGastoEnDashboard = true)
                        showPaymentConfirmDialog = false
                        navController.popBackStack() // Opcional: Regresa a la lista de tarjetas
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

@Composable
fun MsiMinimalItem(
    compra: CompraMSI,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progreso = if (compra.cuotasTotales > 0) compra.cuotasPagadas.toFloat() / compra.cuotasTotales else 0f
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .pointerInput(Unit) { detectTapGestures(onLongPress = { showMenu = true }) }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(compra.descripcion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (compra.deudor != "Yo") {
                        Text("Paga: ${compra.deudor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text("$${String.format("%,.0f", compra.montoTotalOriginal)}", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra super fina
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)), // Barra muy fina y elegante
                    color = if(progreso >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "${compra.cuotasPagadas}/${compra.cuotasTotales}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f)
                )
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Editar") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
            DropdownMenuItem(text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MsiFormDialog(
    titulo: String,
    compraInicial: CompraMSI? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    var desc by remember { mutableStateOf(compraInicial?.descripcion ?: "") }
    var total by remember { mutableStateOf(compraInicial?.montoTotalOriginal?.toString() ?: "") }
    var cuotas by remember { mutableStateOf(compraInicial?.cuotasTotales?.toString() ?: "") }
    var deudor by remember { mutableStateOf(if (compraInicial?.deudor == "Yo") "" else compraInicial?.deudor ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                titulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("¿Qué compraste?") },
                placeholder = { Text("Ej: iPhone 15") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = total,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) total = it },
                    label = { Text("Monto Total ($)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = cuotas,
                    onValueChange = { if (it.all { c -> c.isDigit() }) cuotas = it },
                    label = { Text("Meses") }, // Texto corto
                    modifier = Modifier.weight(0.5f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Column {
                OutlinedTextField(
                    value = deudor,
                    onValueChange = { deudor = it },
                    label = { Text("¿Quién paga? (Opcional)") },
                    placeholder = { Text("Déjalo vacío si eres tú") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                // Sugerencias rápidas
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(onClick = { deudor = "Mamá" }, label = { Text("Mamá") })
                    SuggestionChip(onClick = { deudor = "Pareja" }, label = { Text("Pareja") })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (desc.isNotEmpty() && total.isNotEmpty() && cuotas.isNotEmpty()) {
                        val quienPaga = if (deudor.isBlank()) "Yo" else deudor
                        onConfirm(desc, total.toDouble(), cuotas.toInt(), quienPaga)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Guardar Compra")
            }
        }
    }
}