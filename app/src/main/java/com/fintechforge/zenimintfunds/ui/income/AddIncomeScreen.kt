package com.fintechforge.zenimintfunds.ui.income

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.ui.expenses.CategoryEmojiChip
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeScreen(viewModel: FinanceViewModel, navController: NavController) {
    var monto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    // --- MAGIA: Leemos las categorías de la Base de Datos ---
    val categoriasBD by viewModel.categoriasIngreso.collectAsState()
    var categoriaSeleccionada by remember { mutableStateOf("") }

    // Seleccionar la primera por defecto al cargar
    LaunchedEffect(categoriasBD) {
        if (categoriaSeleccionada.isEmpty() && categoriasBD.isNotEmpty()) {
            categoriaSeleccionada = categoriasBD[0].nombre
        }
    }

    var esRecurrente by remember { mutableStateOf(false) }
    var frecuencia by remember { mutableStateOf("Mensual") }

    var esQuincenaTradicional by remember { mutableStateOf(true) }

    var diaSemana by remember { mutableStateOf(Calendar.MONDAY) }
    var diaMes1 by remember { mutableStateOf("15") }
    var diaMes2 by remember { mutableStateOf("30") }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ingreso Recibido", style = MaterialTheme.typography.titleMedium, color = Color.Gray)

            // --- INPUT GIGANTE Y ELEGANTE ($) ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Text(
                    text = "$",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    color = if(monto.isEmpty()) Color.LightGray else MaterialTheme.colorScheme.primary
                )
                BasicTextField(
                    value = monto,
                    onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() || c == '.' }) monto = it },
                    textStyle = TextStyle(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(IntrinsicSize.Min),
                    decorationBox = { innerTextField ->
                        if (monto.isEmpty()) {
                            Text("0", fontSize = 64.sp, fontWeight = FontWeight.Light, color = Color.LightGray)
                        } else {
                            innerTextField()
                        }
                    }
                )
            }

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                placeholder = { Text("Concepto (Ej: Quincena)", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)
                ),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- GRID DE CATEGORÍAS DINÁMICO ---
            Text("Origen del Dinero", modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().height(220.dp),
                userScrollEnabled = false
            ) {
                items(categoriasBD) { cat ->
                    // Usamos el nuevo componente dinámico de Emojis
                    CategoryEmojiChip(
                        nombre = cat.nombre,
                        iconoEmoji = cat.icono,
                        isSelected = categoriaSeleccionada == cat.nombre,
                        onClick = { categoriaSeleccionada = cat.nombre },
                        colorFondo = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
                        colorActivo = Color(cat.colorHex)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN DE AUTOMATIZACIÓN ---
            Surface(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Automatizar Ingreso", fontWeight = FontWeight.Bold)
                            Text("La app lo agregará sola", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Switch(checked = esRecurrente, onCheckedChange = { esRecurrente = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha=0.5f)))
                    }

                    if (esRecurrente) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("Semanal", "Quincenal", "Mensual").forEach { opcion ->
                                FilterChip(
                                    selected = frecuencia == opcion, onClick = { frecuencia = opcion }, label = { Text(opcion) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha=0.2f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when (frecuencia) {
                            "Semanal" -> {
                                Text("¿Qué día te pagan?", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    val dias = listOf("D" to Calendar.SUNDAY, "L" to Calendar.MONDAY, "M" to Calendar.TUESDAY, "M" to Calendar.WEDNESDAY, "J" to Calendar.THURSDAY, "V" to Calendar.FRIDAY, "S" to Calendar.SATURDAY)
                                    dias.forEach { (letra, valor) ->
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                                .background(if (diaSemana == valor) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable { diaSemana = valor },
                                            contentAlignment = Alignment.Center
                                        ) { Text(letra, color = if (diaSemana == valor) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                            "Quincenal" -> {
                                Text("¿Qué días te pagan?", style = MaterialTheme.typography.labelMedium, color = Color.Gray)

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                    Checkbox(
                                        checked = esQuincenaTradicional,
                                        onCheckedChange = { esQuincenaTradicional = it },
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Text("Tradicional (15 y Fin de mes)", style = MaterialTheme.typography.bodyMedium)
                                }

                                if (!esQuincenaTradicional) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                                        OutlinedTextField(value = diaMes1, onValueChange = { if(it.length <= 2) diaMes1 = it }, label = { Text("Día 1") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                                        OutlinedTextField(value = diaMes2, onValueChange = { if(it.length <= 2) diaMes2 = it }, label = { Text("Día 2") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                                    }
                                }
                            }
                            "Mensual" -> {
                                Text("¿Qué día te pagan?", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                OutlinedTextField(value = diaMes1, onValueChange = { if(it.length <= 2) diaMes1 = it }, label = { Text("Día del mes") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (monto.isNotEmpty()) {
                        val descFinal = if (descripcion.isBlank()) categoriaSeleccionada else descripcion

                        val d1 = when {
                            frecuencia == "Semanal" -> diaSemana
                            frecuencia == "Quincenal" && esQuincenaTradicional -> 15
                            else -> diaMes1.toIntOrNull() ?: 1
                        }
                        val d2 = when {
                            frecuencia == "Quincenal" && esQuincenaTradicional -> 31
                            frecuencia == "Quincenal" -> diaMes2.toIntOrNull() ?: 30
                            else -> null
                        }

                        viewModel.agregarIngreso(monto.toDouble(), descFinal, categoriaSeleccionada, esRecurrente, frecuencia, d1, d2)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = monto.isNotEmpty()
            ) {
                Text("Registrar Ingreso", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}