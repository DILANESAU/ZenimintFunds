package com.fintechforge.zenimintfunds.ui.income

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel
import com.fintechforge.zenimintfunds.data.FrecuenciaPago

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeScreen(viewModel: FinanceViewModel, navController: NavController) {
    var monto by remember { mutableStateOf("") }
    var fuente by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) } // Para el menú desplegable
    var frecuenciaSeleccionada by remember { mutableStateOf(FrecuenciaPago.MENSUAL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Ingreso") },
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
            Text("Registra tu salario o ingresos extra.", style = MaterialTheme.typography.bodyMedium)

            // Campo Monto
            OutlinedTextField(
                value = monto,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) monto = it },
                label = { Text("Monto ($)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Campo Fuente (Ej: Nómina, Freelance)
            OutlinedTextField(
                value = fuente,
                onValueChange = { fuente = it },
                label = { Text("Fuente (ej: Sueldo)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Selector de Frecuencia (Dropdown Menu)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = frecuenciaSeleccionada.name,
                    onValueChange = {},
                    label = { Text("Frecuencia") },
                    modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                    enabled = false, // Deshabilitado para que solo funcione el click del Box
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Opciones") },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                // El menú invisible que aparece al dar click
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    FrecuenciaPago.values().forEach { frecuencia ->
                        DropdownMenuItem(
                            text = { Text(frecuencia.name) },
                            onClick = {
                                frecuenciaSeleccionada = frecuencia
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (monto.isNotEmpty() && fuente.isNotEmpty()) {
                        viewModel.agregarIngreso(monto.toDouble(), fuente, frecuenciaSeleccionada)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar Ingreso")
            }
        }
    }
}