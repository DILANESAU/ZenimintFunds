package com.fintechforge.zenimintfunds.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    var categoriaSeleccionada by remember { mutableStateOf("General") }

    val categorias = listOf(
        CategoryItem("Comida", Icons.Default.Restaurant),
        CategoryItem("Transporte", Icons.Default.DirectionsCar),
        CategoryItem("Ocio", Icons.Default.Movie),
        CategoryItem("Hogar", Icons.Default.Home),
        CategoryItem("Salud", Icons.Default.MedicalServices),
        CategoryItem("Ropa", Icons.Default.Checkroom)
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Registrar Gasto") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 1. INPUT MONTO GIGANTE
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("¿Cuánto gastaste?", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                TextField(
                    value = monto,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) monto = it },
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 56.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    placeholder = {
                        Text("$0", style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, color = Color.LightGray, textAlign = TextAlign.Center))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // 2. SELECTOR DE CATEGORÍA (CARRUSEL)
            Column {
                Text("Categoría", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(categorias) { cat ->
                        CategoryChip(
                            item = cat,
                            isSelected = categoriaSeleccionada == cat.name,
                            onClick = { categoriaSeleccionada = cat.name }
                        )
                    }
                }
            }

            // 3. DESCRIPCIÓN (Opcional)
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Nota (Opcional)") },
                placeholder = { Text("Ej: Tacos con amigos") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // BOTÓN FLOTANTE GRANDE
            Button(
                onClick = {
                    if (monto.isNotEmpty()) {
                        val descFinal = if (descripcion.isBlank()) categoriaSeleccionada else descripcion
                        viewModel.agregarGasto(monto.toDouble(), descFinal, categoriaSeleccionada)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // Rojo para gasto
            ) {
                Text("Guardar Gasto", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Data class auxiliar y componentes
data class CategoryItem(val name: String, val icon: ImageVector)

@Composable
fun CategoryChip(item: CategoryItem, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}