package com.fintechforge.zenimintfunds.ui.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(viewModel: FinanceViewModel, navController: NavController) {
    var banco by remember { mutableStateOf("") }
    var limite by remember { mutableStateOf("") }
    var diaCorte by remember { mutableStateOf("") }
    var diaPago by remember { mutableStateOf("") }

    // --- MAGIA: Detectar el banco para cambiar el color ---
    val colorBase = when (banco.lowercase().trim()) {
        "nu", "nubank" -> Color(0xFF8A05BE)
        "bbva", "bancomer" -> Color(0xFF004481)
        "santander" -> Color(0xFFEC0000)
        "amex", "american express" -> Color(0xFF006FCF)
        "stori" -> Color(0xFF00C9A7)
        "rappi", "rappicard" -> Color(0xFFFF441F)
        "hey", "hey banco" -> Color(0xFF000000)
        "banamex", "citi" -> Color(0xFF002D72)
        "hsbc" -> Color(0xFFDB0011)
        else -> MaterialTheme.colorScheme.primary // Color por defecto (Menta)
    }

    // Animación suave de transición de color
    val animatedColor by animateColorAsState(
        targetValue = colorBase,
        animationSpec = tween(durationMillis = 500),
        label = "ColorTarjeta"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Tarjeta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.Close, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // --- 1. VISTA PREVIA MINIMALISTA EN TIEMPO REAL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(animatedColor.copy(alpha = 0.8f), animatedColor)
                        )
                    )
                    .padding(24.dp)
            ) {
                // Nombre del Banco
                Text(
                    text = if (banco.isEmpty()) "Nombre del Banco" else banco.uppercase(),
                    color = Color.White.copy(alpha = if (banco.isEmpty()) 0.5f else 1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Límite de Crédito
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text("Línea de Crédito", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = if (limite.isEmpty()) "$0.00" else "$${String.format("%,.0f", limite.toDoubleOrNull() ?: 0.0)}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Fechas
                Column(modifier = Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.End) {
                    Text("Corte: ${if (diaCorte.isEmpty()) "--" else diaCorte}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                    Text("Pago: ${if (diaPago.isEmpty()) "--" else diaPago}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 2. FORMULARIO ELEGANTE ---
            OutlinedTextField(
                value = banco,
                onValueChange = { banco = it },
                label = { Text("Banco (Ej: Nu, BBVA, Amex)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = limite,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) limite = it },
                label = { Text("Límite de Crédito ($)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = diaCorte,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) diaCorte = it },
                    label = { Text("Día de Corte") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = diaPago,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) diaPago = it },
                    label = { Text("Día de Pago") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- 3. BOTÓN DE GUARDAR ---
            Button(
                onClick = {
                    if (banco.isNotEmpty() && limite.isNotEmpty()) {
                        viewModel.agregarTarjeta(
                            banco,
                            limite.toDoubleOrNull() ?: 0.0,
                            diaCorte.toIntOrNull() ?: 1,
                            diaPago.toIntOrNull() ?: 10
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = banco.isNotEmpty() && limite.isNotEmpty()
            ) {
                Text("Guardar Tarjeta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}