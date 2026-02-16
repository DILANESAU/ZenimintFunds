package com.fintechforge.zenimintfunds.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 1. Definimos qué propiedades tiene un "Estilo de Banco"
data class BankStyle(
    val nombreBanco: String,
    val brush: Brush,
    val textColor: Color = Color.White,
    val logoText: String // Texto corto para el logo (ej: "Nu", "BBVA")
)

// 2. Nuestro "Cerebro" que decide qué estilo usar
object BankBranding {

    // Estilo por defecto (Gris Elegante - Zenimint)
    private val DefaultStyle = BankStyle(
        nombreBanco = "Genérico",
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF232526), Color(0xFF414345)) // Negro Carbón
        ),
        logoText = "CARD"
    )

    // 3. La función mágica que busca el estilo
    fun getStyleFor(nombreBancoInput: String): BankStyle {
        // Normalizamos el texto (quitamos mayúsculas/minúsculas y espacios extra)
        val banco = nombreBancoInput.trim().lowercase()

        return when {
            // NU (Morado)
            banco.contains("nu") -> BankStyle(
                nombreBanco = "Nu",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF820AD1), Color(0xFF4A00E0))
                ),
                logoText = "Nu"
            )
            // BBVA (Azul Marino)
            banco.contains("bbva") || banco.contains("bancomer") -> BankStyle(
                nombreBanco = "BBVA",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF004481), Color(0xFF1464A5))
                ),
                logoText = "BBVA"
            )
            // SANTANDER (Rojo Fuego)
            banco.contains("santander") -> BankStyle(
                nombreBanco = "Santander",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFEC0000), Color(0xFFB00000))
                ),
                logoText = "Santander"
            )
            // CITIBANAMEX (Azul Clásico)
            banco.contains("banamex") || banco.contains("citi") -> BankStyle(
                nombreBanco = "Citibanamex",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF002D72), Color(0xFF005BEA))
                ),
                logoText = "Citi"
            )
            // KLAR (Transparente / Colorido)
            banco.contains("klar") -> BankStyle(
                nombreBanco = "Klar",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFF2979FF)) // Cyan a Azul
                ),
                logoText = "Klar",
                textColor = Color.Black // Klar suele ser claro, cambiamos texto a negro
            )
            // STORI (Verde)
            banco.contains("stori") -> BankStyle(
                nombreBanco = "Stori",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF00A19B), Color(0xFF00796B))
                ),
                logoText = "Stori"
            )
            // RAPPI (Negro con Naranja - Bigote)
            banco.contains("rappi") -> BankStyle(
                nombreBanco = "RappiCard",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF111111), Color(0xFF333333)) // Negro mate
                ),
                logoText = "Rappi"
            )
            // HEY BANCO (Negro minimalista)
            banco.contains("hey") -> BankStyle(
                nombreBanco = "Hey Banco",
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF000000), Color(0xFF1C1C1C))
                ),
                logoText = "hey"
            )

            // Si no lo conocemos, regresamos el Default
            else -> DefaultStyle.copy(logoText = nombreBancoInput.take(4).uppercase())
        }
    }
}