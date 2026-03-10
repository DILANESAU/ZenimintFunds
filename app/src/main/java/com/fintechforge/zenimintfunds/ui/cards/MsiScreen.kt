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
import androidx.compose.foundation.verticalScroll
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
    val gastosDirectos by viewModel.obtenerGastosDirectosTarjeta(tarjetaId).collectAsState(initial = emptyList())
    val deudores by viewModel.deudores.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showAddDialog by remember { mutableStateOf(false) }
    var compraAEditar by remember { mutableStateOf<CompraMSI?>(null) }
    var showPaymentConfirmDialog by remember { mutableStateOf(false) }

    // --- CÁLCULOS MATEMÁTICOS ---
    val pagoMsi = compras.sumOf { it.montoTotalOriginal / it.cuotasTotales }
    val pagoDirecto = gastosDirectos.sumOf { it.monto }
    val pagoTotalAlBanco = pagoMsi + pagoDirecto // Suma de ambos mundos

    // Tu deuda personal (MSI de "Yo" + Todos los gastos directos que por definición son tuyos)
    val miDeudaMensual = compras.filter { it.deudor == "Yo" }.sumOf { it.montoTotalOriginal / it.cuotasTotales } + pagoDirecto

    // Lo que otros te deben (Solo aplica para MSI en esta lógica)
    val cobranzaMensual = compras.filter { it.deudor != "Yo" }
        .groupBy { it.deudor }
        .mapValues { entry -> entry.value.sumOf { it.montoTotalOriginal / it.cuotasTotales } }

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
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, shape = CircleShape) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Compra")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // --- HEADER: TOTAL A PAGAR ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total a pagar este mes", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(
                        "$${String.format("%,.2f", pagoTotalAlBanco)}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    )

                    if (pagoDirecto > 0) {
                        Text(
                            "Includes $${String.format("%,.0f", pagoDirecto)} de gastos directos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (miDeudaMensual < pagoTotalAlBanco) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Tu parte real: $${String.format("%,.2f", miDeudaMensual)}") },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            border = null
                        )
                    }
                }
            }

            // --- BOTÓN DE REGISTRO ---
            if (pagoTotalAlBanco > 0) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Button(
                            onClick = { showPaymentConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Pagar Tarjeta ($${String.format("%,.0f", pagoTotalAlBanco)})", fontSize = 16.sp)
                        }
                    }
                }
            }

            // --- SECCIÓN: COBRANZA ---
            if (cobranzaMensual.isNotEmpty()) {
                item {
                    Text("Recuperar de amigos", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.primary)
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cobranzaMensual.forEach { (nombre, monto) ->
                            FilterChip(
                                selected = true, onClick = {},
                                label = { Text("$nombre: $${String.format("%,.0f", monto)}") },
                                leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            // --- SECCIÓN: MSI (DESGLOSE) ---
            if (compras.isNotEmpty()) {
                item {
                    Text("Compras a Meses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
                }
                items(compras) { compra ->
                    MsiMinimalItem(compra = compra, onEdit = { compraAEditar = compra }, onDelete = { viewModel.borrarCompra(compra) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }

            // --- SECCIÓN: GASTOS DIRECTOS DEL MES ---
            if (gastosDirectos.isNotEmpty()) {
                item {
                    Text("Gastos Directos (Este mes)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
                }
                items(gastosDirectos) { gasto ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(gasto.descripcion, style = MaterialTheme.typography.bodyLarge)
                            Text(gasto.categoria, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Text("$${String.format("%,.2f", gasto.monto)}", fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                }
            }
        }
    }

    // --- DIÁLOGOS (Se mantienen igual, solo actualicé el texto de confirmación) ---
    if (showPaymentConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentConfirmDialog = false },
            title = { Text("Confirmar Pago") },
            text = { Text("Se liquidarán tus gastos directos y se sumará un mes a tus MSI.\n\nTotal: $${String.format("%,.2f", pagoTotalAlBanco)}") },
            confirmButton = {

                Button(onClick = {
                    viewModel.pagarMensualidadTarjeta(tarjetaId, crearGastoEnDashboard = true)
                    showPaymentConfirmDialog = false
                    navController.popBackStack()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                    Text("Confirmar Pago")
                }
            },
            dismissButton = { TextButton(onClick = { showPaymentConfirmDialog = false }) { Text("Cancelar") } }
        )
    }


    if (showAddDialog) {
        MsiFormDialog(
            titulo = "Nueva Compra",
            deudores = deudores,
            onDismiss = { showAddDialog = false },
            onConfirm = { desc, total, cuotas, deudor, pagadas ->
                viewModel.agregarCompraMSI(tarjetaId, desc, total, cuotas, deudor, pagadas)
                showAddDialog = false
            }
        )
    }

    // EN EDITAR:
    if (compraAEditar != null) {
        MsiFormDialog(
            titulo = "Editar Compra",
            compraInicial = compraAEditar,
            onDismiss = { compraAEditar = null },
            deudores = deudores,
            onConfirm = { desc, total, cuotas, deudor, pagadas ->
                viewModel.actualizarCompra(compraAEditar!!.copy(
                    descripcion = desc, montoTotalOriginal = total, cuotasTotales = cuotas, deudor = deudor, cuotasPagadas = pagadas
                ))
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
    compraInicial: com.fintechforge.zenimintfunds.data.CompraMSI? = null,
    deudores: List<com.fintechforge.zenimintfunds.data.Deudor>, // <-- NUEVO PARÁMETRO
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var desc by remember { mutableStateOf(compraInicial?.descripcion ?: "") }
    var total by remember { mutableStateOf(compraInicial?.montoTotalOriginal?.toString() ?: "") }
    var cuotasTotales by remember { mutableStateOf(compraInicial?.cuotasTotales?.toString() ?: "") }
    var cuotasPagadas by remember { mutableStateOf(compraInicial?.cuotasPagadas?.toString() ?: "") }
    var deudor by remember { mutableStateOf(compraInicial?.deudor ?: "Yo") }

    // Estado del menú desplegable
    var expandedMenu by remember { mutableStateOf(false) }
    // Creamos la lista de opciones: Siempre incluye "Yo" de primero, luego los amigos
    val opcionesDeudor = listOf("Yo") + deudores.map { it.nombre }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("¿Qué compraste?") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = total, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) total = it }, label = { Text("Monto Total ($)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = cuotasTotales, onValueChange = { if (it.all { c -> c.isDigit() }) cuotasTotales = it }, label = { Text("Total Meses") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = cuotasPagadas, onValueChange = { if (it.all { c -> c.isDigit() }) cuotasPagadas = it }, label = { Text("Ya pagados") }, placeholder = { Text("Ej: 8") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
            }

            // --- EL MENÚ DESPLEGABLE MÁGICO ---
            ExposedDropdownMenuBox(
                expanded = expandedMenu,
                onExpandedChange = { expandedMenu = !expandedMenu }
            ) {
                OutlinedTextField(
                    value = deudor,
                    onValueChange = {},
                    readOnly = true, // Evita que se abra el teclado
                    label = { Text("¿Quién lo paga?") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    opcionesDeudor.forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion) },
                            onClick = {
                                deudor = opcion
                                expandedMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (desc.isNotEmpty() && total.isNotEmpty() && cuotasTotales.isNotEmpty()) {
                        val pagadas = cuotasPagadas.toIntOrNull() ?: 0
                        onConfirm(desc, total.toDouble(), cuotasTotales.toInt(), deudor, pagadas)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Guardar Compra") }
        }
    }
}