package com.fintechforge.zenimintfunds.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.data.TarjetaCredito
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(viewModel: FinanceViewModel, navController: NavController) {
    val tarjetas by viewModel.tarjetas.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var tarjetaAEditar by remember { mutableStateOf<TarjetaCredito?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Panel de Crédito", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(tarjetas) { tarjeta ->
                AnalyticCardItem(
                    tarjeta = tarjeta,
                    viewModel = viewModel,
                    onClick = { navController.navigate("msi_details/${tarjeta.id}") },
                    onEdit = {
                        tarjetaAEditar = tarjeta
                        showEditDialog = true
                    },
                    onDelete = { viewModel.borrarTarjeta(tarjeta) }
                )
            }

            item { AddCardGhostButton(onClick = { navController.navigate("add_card") }) }
        }
    }

    if (showEditDialog && tarjetaAEditar != null) {
        EditCardDialog(
            tarjeta = tarjetaAEditar!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { banco, limite, corte, pago ->
                viewModel.actualizarTarjeta(tarjetaAEditar!!.copy(nombreBanco = banco, limiteCredito = limite, diaCorte = corte, diaLimitePago = pago))
                showEditDialog = false
            }
        )
    }
}

@Composable
fun AnalyticCardItem(
    tarjeta: TarjetaCredito,
    viewModel: FinanceViewModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Escuchamos las compras de ESTA tarjeta para calcular su deuda en tiempo real
    val compras by viewModel.obtenerComprasPorTarjeta(tarjeta.id).collectAsState(initial = emptyList())

    val deudaMSI = compras.sumOf { it.saldoRestante() }
    val creditoDisponible = tarjeta.limiteCredito - deudaMSI
    val porcentajeUso = if (tarjeta.limiteCredito > 0) (deudaMSI / tarjeta.limiteCredito).toFloat() else 0f

    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures(onLongPress = { showMenu = true }, onTap = { onClick() }) },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header: Nombre y Fechas
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(tarjeta.nombreBanco.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Corte: ${tarjeta.diaCorte}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("Pago: ${tarjeta.diaLimitePago}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Números
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Deuda MSI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        Text("$${String.format("%,.0f", deudaMSI)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Disponible", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("$${String.format("%,.0f", creditoDisponible)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barra de progreso de crédito
                LinearProgressIndicator(
                    progress = { porcentajeUso },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = if (porcentajeUso > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Línea Total: $${String.format("%,.0f", tarjeta.limiteCredito)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Editar") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
            DropdownMenuItem(text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
        }
    }
}

// ... (Mantén tu función AddCardGhostButton igual)
@Composable
fun AddCardGhostButton(onClick: () -> Unit) {
    val stroke = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .drawBehind { drawRoundRect(color = primaryColor.copy(alpha = 0.5f), style = stroke, cornerRadius = CornerRadius(20.dp.toPx())) },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar otra tarjeta", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

// Diálogo para editar tarjetas
@Composable
fun EditCardDialog(tarjeta: TarjetaCredito, onDismiss: () -> Unit, onConfirm: (String, Double, Int, Int) -> Unit) {
    var banco by remember { mutableStateOf(tarjeta.nombreBanco) }
    var limite by remember { mutableStateOf(tarjeta.limiteCredito.toString()) }
    var diaCorte by remember { mutableStateOf(tarjeta.diaCorte.toString()) }
    var diaPago by remember { mutableStateOf(tarjeta.diaLimitePago.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Tarjeta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = banco, onValueChange = { banco = it }, label = { Text("Banco") })
                OutlinedTextField(value = limite, onValueChange = { limite = it }, label = { Text("Límite") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = diaCorte, onValueChange = { diaCorte = it }, label = { Text("Corte") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = diaPago, onValueChange = { diaPago = it }, label = { Text("Pago") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = { Button(onClick = { if (banco.isNotEmpty() && limite.isNotEmpty()) onConfirm(banco, limite.toDouble(), diaCorte.toInt(), diaPago.toInt()) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}