package com.fintechforge.zenimintfunds.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fintechforge.zenimintfunds.viewmodel.FinanceViewModel
import com.fintechforge.zenimintfunds.data.TarjetaCredito
import com.fintechforge.zenimintfunds.ui.theme.BankBranding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(viewModel: FinanceViewModel, navController: NavController) {
    val tarjetas by viewModel.tarjetas.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Billetera", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(24.dp), // Buen espacio entre tarjetas
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // LISTA DE TARJETAS
            items(tarjetas) { tarjeta ->
                TarjetaVisualItem(
                    tarjeta = tarjeta,
                    onClick = { navController.navigate("msi_details/${tarjeta.id}") }
                )
            }

            // BOTÓN "AGREGAR NUEVA" (Estilo Fantasma / Punteado)
            item {
                AddCardGhostButton(onClick = { navController.navigate("add_card") })
            }
        }
    }
}

// --- COMPONENTES VISUALES ---

@Composable
fun TarjetaVisualItem(tarjeta: TarjetaCredito, onClick: () -> Unit) {
    // Usamos tu sistema de Branding
    val estilo = BankBranding.getStyleFor(tarjeta.nombreBanco)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp) // Un poco más alta para presencia
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp), // Bordes más redondeados (Moderno)
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(estilo.brush)
                .padding(28.dp) // Más padding interno
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Banco y Contactless
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = estilo.logoText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = estilo.textColor.copy(alpha = 0.9f),
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = estilo.textColor.copy(alpha = 0.5f)
                    )
                }

                // Centro: Chip simulado
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp, 38.dp)
                            .background(
                                color = Color(0xFFFFD700).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "••••  ${tarjeta.id.toString().padStart(4, '0')}",
                        style = MaterialTheme.typography.titleLarge,
                        color = estilo.textColor.copy(alpha = 0.9f),
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Footer: Info Financiera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("CORTE EL ${tarjeta.diaCorte}", color = estilo.textColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("PAGA EL ${tarjeta.diaLimitePago}", color = estilo.textColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$${String.format("%,.0f", tarjeta.limiteCredito)}",
                        color = estilo.textColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

@Composable
fun AddCardGhostButton(onClick: () -> Unit) {
    val stroke = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .drawBehind {
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.5f),
                    style = stroke,
                    cornerRadius = CornerRadius(24.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar otra tarjeta", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}