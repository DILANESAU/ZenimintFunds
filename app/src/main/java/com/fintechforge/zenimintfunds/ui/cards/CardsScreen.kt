package com.fintechforge.zenimintfunds.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel
import com.fintechforge.zenimintfunds.data.TarjetaCredito

// --- PANTALLA LISTA DE TARJETAS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(viewModel: FinanceViewModel, navController: NavController) {
    val tarjetas by viewModel.tarjetas.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Billetera", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_card") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Tarjeta")
            }
        }
    ) { padding ->
        if (tarjetas.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No tienes tarjetas. Agrega una con el botón +", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(tarjetas) { tarjeta ->
                    // Al hacer click, vamos al detalle de MSI pasando el ID
                    TarjetaVisualItem(
                        tarjeta = tarjeta,
                        onClick = { navController.navigate("msi_details/${tarjeta.id}") }
                    )
                }
            }
        }
    }
}

// --- COMPONENTE VISUAL DE TARJETA (ESTILO PREMIUM) ---
@Composable
fun TarjetaVisualItem(tarjeta: TarjetaCredito, onClick: () -> Unit) {
    // Lógica para elegir color según el ID (para que varíe)
    val gradient = when (tarjeta.id % 3) {
        0 -> Brush.linearGradient(listOf(Color(0xFF232526), Color(0xFF414345))) // Black Card
        1 -> Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))) // Blue Steel
        else -> Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))) // Neon Purple
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() }, // Hacemos toda la tarjeta clickeable
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Parte Superior: Banco y Contactless
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tarjeta.nombreBanco.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Contactless",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Parte Central: Chip y Terminación
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Dibujamos un chip falso
                    Box(
                        modifier = Modifier
                            .size(45.dp, 34.dp)
                            .background(Color(0xFFFFD700).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        // Ocultamos el ID real y mostramos ceros para simular
                        text = "**** **** **** ${tarjeta.id.toString().padStart(4, '0')}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                        letterSpacing = 3.sp
                    )
                }

                // Parte Inferior: Fechas y Límite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("CORTE / PAGO", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${tarjeta.diaCorte} / ${tarjeta.diaLimitePago}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("LÍMITE", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "$${String.format("%,.2f", tarjeta.limiteCredito)}", // Formato moneda
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
}

// --- PANTALLA FORMULARIO (ADD CARD) ---
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Registra tu tarjeta para controlar tus fechas y MSI.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            OutlinedTextField(
                value = banco,
                onValueChange = { banco = it },
                label = { Text("Nombre del Banco (ej: Nu, BBVA)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = limite,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) limite = it },
                label = { Text("Límite de Crédito ($)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = diaCorte,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) diaCorte = it },
                    label = { Text("Día Corte") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = diaPago,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) diaPago = it },
                    label = { Text("Día Pago") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar Tarjeta")
            }
        }
    }
}