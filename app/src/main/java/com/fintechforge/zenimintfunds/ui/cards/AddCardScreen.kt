package com.fintechforge.zenimintfunds.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.data.TarjetaCredito
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(viewModel: FinanceViewModel, navController: NavController) {
    var banco by remember { mutableStateOf("") }
    var limite by remember { mutableStateOf("") }
    var diaCorte by remember { mutableStateOf("") }
    var diaPago by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Tarjeta") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 1. VISTA PREVIA EN TIEMPO REAL
            // Creamos una tarjeta "falsa" temporal para mostrar cómo se vería
            val tarjetaPreview = TarjetaCredito(
                nombreBanco = if (banco.isEmpty()) "Tu Banco" else banco,
                limiteCredito = limite.toDoubleOrNull() ?: 0.0,
                diaCorte = diaCorte.toIntOrNull() ?: 1,
                diaLimitePago = diaPago.toIntOrNull() ?: 10
            )

            Text("Vista Previa", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            TarjetaVisualItem(tarjeta = tarjetaPreview, onClick = {}) // Reutilizamos tu componente visual

            // 2. FORMULARIO
            OutlinedTextField(
                value = banco,
                onValueChange = { banco = it },
                label = { Text("Nombre del Banco") },
                placeholder = { Text("Ej: Nu, BBVA...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = limite,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) limite = it },
                label = { Text("Límite de Crédito") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                prefix = { Text("$ ") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = diaCorte,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) diaCorte = it },
                    label = { Text("Día Corte") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = diaPago,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) diaPago = it },
                    label = { Text("Día Pago") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

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
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = banco.isNotEmpty() && limite.isNotEmpty()
            ) {
                Text("Crear Tarjeta")
            }
        }
    }
}