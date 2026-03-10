package com.fintechforge.zenimintfunds.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.data.Deudor
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: FinanceViewModel, navController: NavController) {
    val deudores by viewModel.deudores.collectAsState()
    val ingresos by viewModel.ingresos.collectAsState()

    // --- CAMBIO VERSIÓN 7: Listas separadas ---
    val categoriasGasto by viewModel.categoriasGasto.collectAsState()
    val categoriasIngreso by viewModel.categoriasIngreso.collectAsState()

    var showDebtorsDialog by remember { mutableStateOf(false) }
    var showIncomesSheet by remember { mutableStateOf(false) }
    var showCategoriesSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- PERFIL ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("U", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Usuario Zenimint", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Bóveda Segura Activa", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // --- GESTIÓN ---
            Text("Gestión", style = MaterialTheme.typography.labelLarge, color = Color.Gray)

            SettingsOption(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Mis Ingresos",
                subtitle = "Edita salarios y automatizaciones",
                iconColor = Color(0xFF69F0AE),
                onClick = { showIncomesSheet = true }
            )

            SettingsOption(
                icon = Icons.Default.Group,
                title = "Amigos y Deudores",
                subtitle = "${deudores.size} registrados",
                iconColor = Color(0xFF42A5F5),
                onClick = { showDebtorsDialog = true }
            )

            SettingsOption(
                icon = Icons.Default.Category,
                title = "Categorías Personalizadas",
                subtitle = "${categoriasGasto.size + categoriasIngreso.size} etiquetas personalizadas",
                iconColor = Color(0xFFAB47BC),
                onClick = { showCategoriesSheet = true }
            )

            Text("Preferencias", style = MaterialTheme.typography.labelLarge, color = Color.Gray)

            SettingsOption(
                icon = Icons.Default.Fingerprint,
                title = "Seguridad Biométrica",
                subtitle = "Bloqueo con huella al entrar",
                iconColor = Color(0xFF00BFA5),
                onClick = { /* TODO */ }
            )
            SettingsOption(
                icon = Icons.Default.Notifications,
                title = "Notificaciones",
                subtitle = "Alertas de corte y automatización",
                iconColor = Color(0xFFFFCA28),
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- DIÁLOGOS Y HOJAS ---
        if (showDebtorsDialog) {
            ManageDebtorsDialog(
                deudores = deudores,
                onDismiss = { showDebtorsDialog = false },
                onAdd = { viewModel.agregarDeudor(it) },
                onDelete = { viewModel.eliminarDeudor(it) }
            )
        }

        if (showIncomesSheet) {
            ManageIncomesSheet(
                ingresos = ingresos,
                onDismiss = { showIncomesSheet = false },
                onDelete = { viewModel.eliminarIngreso(it) },
                onEdit = { ingresoViejo, nuevoMonto, nuevaDesc ->
                    viewModel.actualizarIngreso(ingresoViejo.copy(monto = nuevoMonto, descripcion = nuevaDesc))
                }
            )
        }

        if (showCategoriesSheet) {
            ManageCategoriesSheet(
                categoriasGasto = categoriasGasto,
                categoriasIngreso = categoriasIngreso,
                onDismiss = { showCategoriesSheet = false },
                onAdd = { nombre, icono, colorHex, tipo ->
                    viewModel.agregarCategoria(nombre, icono, colorHex, tipo)
                },
                onDelete = { viewModel.eliminarCategoria(it) }
            )
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun SettingsOption(icon: ImageVector, title: String, subtitle: String, iconColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = iconColor)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
    }
}

// [ManageDebtorsDialog y ManageIncomesSheet se mantienen igual que en tus versiones anteriores]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesSheet(
    categoriasGasto: List<com.fintechforge.zenimintfunds.data.Categoria>,
    categoriasIngreso: List<com.fintechforge.zenimintfunds.data.Categoria>,
    onDismiss: () -> Unit,
    onAdd: (String, String, Long, String) -> Unit,
    onDelete: (com.fintechforge.zenimintfunds.data.Categoria) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tabSeleccionada by remember { mutableIntStateOf(0) } // 0 = Gasto, 1 = Ingreso
    val tipoActual = if (tabSeleccionada == 0) "Gastos" else "Ingresos"
    val listaActual = if (tabSeleccionada == 0) categoriasGasto else categoriasIngreso

    var nuevoNombre by remember { mutableStateOf("") }
    var nuevoEmoji by remember { mutableStateOf("✨") }

    val coloresHex = listOf(
        0xFFEF5350, 0xFF42A5F5, 0xFFFFCA28, 0xFFAB47BC, 0xFF00BFA5,
        0xFF8D6E63, 0xFFFF7043, 0xFF26A69A, 0xFF5C6BC0, 0xFFD4E157
    )
    var colorSeleccionado by remember { mutableLongStateOf(coloresHex[0]) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).imePadding().padding(bottom = 24.dp)) {
            Text("Personalizar Categorías", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            TabRow(selectedTabIndex = tabSeleccionada, containerColor = Color.Transparent, modifier = Modifier.padding(vertical = 16.dp)) {
                Tab(selected = tabSeleccionada == 0, onClick = { tabSeleccionada = 0 }, text = { Text("Gastos") })
                Tab(selected = tabSeleccionada == 1, onClick = { tabSeleccionada = 1 }, text = { Text("Ingresos") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nuevoEmoji,
                    onValueChange = { if (it.length <= 2) nuevoEmoji = it },
                    label = { Text("Icono") },
                    modifier = Modifier.width(80.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(coloresHex) { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(40.dp).clip(CircleShape).background(Color(colorHex))
                            .clickable { colorSeleccionado = colorHex }
                            .border(width = if (colorSeleccionado == colorHex) 3.dp else 0.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (nuevoNombre.isNotBlank()) {
                        onAdd(nuevoNombre, nuevoEmoji, colorSeleccionado, tipoActual)
                        nuevoNombre = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar en $tipoActual") }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                items(listaActual) { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).background(Color(cat.colorHex).copy(alpha=0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Text(cat.icono, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(cat.nombre, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { onDelete(cat) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color(0xFFEF5350))
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ManageDebtorsDialog(
    deudores: List<com.fintechforge.zenimintfunds.data.Deudor>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (com.fintechforge.zenimintfunds.data.Deudor) -> Unit
) {
    var nuevoNombre by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Amigos y Deudores", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nuevoNombre,
                        onValueChange = { nuevoNombre = it },
                        label = { Text("Nombre del amigo") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (nuevoNombre.isNotBlank()) {
                                onAdd(nuevoNombre)
                                nuevoNombre = ""
                            }
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(deudores) { deudor ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(deudor.nombre, style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { onDelete(deudor) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color(0xFFEF5350))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageIncomesSheet(
    ingresos: List<com.fintechforge.zenimintfunds.data.Ingreso>,
    onDismiss: () -> Unit,
    onDelete: (com.fintechforge.zenimintfunds.data.Ingreso) -> Unit,
    onEdit: (com.fintechforge.zenimintfunds.data.Ingreso, Double, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var ingresoAEditar by remember { mutableStateOf<com.fintechforge.zenimintfunds.data.Ingreso?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Mis Ingresos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(ingresos) { ingreso ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ingreso.descripcion, fontWeight = FontWeight.Bold)
                            Text("$${String.format("%,.0f", ingreso.monto)}", color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { ingresoAEditar = ingreso }) { Icon(Icons.Default.Edit, null, tint = Color.Gray) }
                        IconButton(onClick = { onDelete(ingreso) }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350)) }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }

    if (ingresoAEditar != null) {
        var montoEdit by remember { mutableStateOf(ingresoAEditar!!.monto.toString()) }
        var descEdit by remember { mutableStateOf(ingresoAEditar!!.descripcion) }

        AlertDialog(
            onDismissRequest = { ingresoAEditar = null },
            title = { Text("Editar Ingreso") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = montoEdit, onValueChange = { montoEdit = it }, label = { Text("Monto") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = descEdit, onValueChange = { descEdit = it }, label = { Text("Nombre") }, shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    onEdit(ingresoAEditar!!, montoEdit.toDoubleOrNull() ?: 0.0, descEdit)
                    ingresoAEditar = null
                }) { Text("Guardar") }
            }
        )
    }
}