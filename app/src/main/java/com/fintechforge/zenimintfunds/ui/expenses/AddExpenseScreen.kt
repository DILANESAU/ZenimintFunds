package com.fintechforge.zenimintfunds.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(viewModel: FinanceViewModel, navController: NavController) {
    var monto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    val categoriasBD by viewModel.categoriasGasto.collectAsState()
    var categoriaSeleccionada by remember { mutableStateOf("") }

    // --- MÉTODOS DE PAGO ---
    val tarjetas by viewModel.tarjetas.collectAsState(initial = emptyList())
    var tarjetaSeleccionadaId by remember { mutableStateOf<Int?>(null) } // null = Efectivo/Débito

    LaunchedEffect(categoriasBD) {
        if (categoriaSeleccionada.isEmpty() && categoriasBD.isNotEmpty()) {
            categoriaSeleccionada = categoriasBD[0].nombre
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text("Monto del Gasto", style = MaterialTheme.typography.titleMedium, color = Color.Gray)

            // --- INPUT GIGANTE ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Text(
                    text = "$",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light,
                    color = if(monto.isEmpty()) Color.LightGray else MaterialTheme.colorScheme.primary
                )
                BasicTextField(
                    value = monto,
                    onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() || c == '.' }) monto = it },
                    textStyle = TextStyle(
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(IntrinsicSize.Min),
                    decorationBox = { innerTextField ->
                        if (monto.isEmpty()) {
                            Text("0", fontSize = 56.sp, fontWeight = FontWeight.Light, color = Color.LightGray)
                        } else {
                            innerTextField()
                        }
                    }
                )
            }

            // --- INPUT NOTA ---
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                placeholder = { Text("¿En qué lo gastaste?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)
                ),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.fillMaxWidth(0.8f).height(48.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN: MÉTODO DE PAGO ---
            Text(
                "¿Cómo pagaste?",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Opción Efectivo
                item {
                    FilterChip(
                        selected = tarjetaSeleccionadaId == null,
                        onClick = { tarjetaSeleccionadaId = null },
                        label = { Text("Efectivo/Débito") },
                        leadingIcon = { Icon(Icons.Default.Payments, null, Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Opciones Tarjetas
                items(tarjetas) { tarjeta ->
                    FilterChip(
                        selected = tarjetaSeleccionadaId == tarjeta.id,
                        onClick = { tarjetaSeleccionadaId = tarjeta.id },
                        label = { Text(tarjeta.nombreBanco) },
                        leadingIcon = { Icon(Icons.Default.CreditCard, null, Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- GRID DE CATEGORÍAS ---
            Text(
                "Categoría",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).padding(top = 8.dp)
            ) {
                items(categoriasBD) { cat ->
                    CategoryEmojiChip(
                        nombre = cat.nombre,
                        iconoEmoji = cat.icono,
                        isSelected = categoriaSeleccionada == cat.nombre,
                        onClick = { categoriaSeleccionada = cat.nombre },
                        colorFondo = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.4f),
                        colorActivo = Color(cat.colorHex)
                    )
                }
            }

            // --- BOTÓN GUARDAR ---
            Button(
                onClick = {
                    if (monto.isNotEmpty()) {
                        val descFinal = if (descripcion.isBlank()) categoriaSeleccionada else descripcion
                        viewModel.agregarGasto(
                            monto.toDouble(),
                            descFinal,
                            categoriaSeleccionada,
                            tarjetaSeleccionadaId
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground),
                enabled = monto.isNotEmpty()
            ) {
                Text("Registrar Gasto", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.background)
            }
        }
    }
}

@Composable
fun CategoryEmojiChip(
    nombre: String,
    iconoEmoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    colorFondo: Color,
    colorActivo: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onClick() }
                .background(if (isSelected) colorActivo.copy(alpha = 0.8f) else colorFondo),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconoEmoji, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = nombre,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
        )
    }
}