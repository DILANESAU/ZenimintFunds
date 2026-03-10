package com.fintechforge.zenimintfunds.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ColumnInfo

enum class FrecuenciaPago {
    SEMANAL, QUINCENAL, MENSUAL
}


@Entity(tableName = "ingresos")
data class Ingreso(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monto: Double,
    val descripcion: String,
    val fechaIngreso: Long,
    val categoria: String = "General",

    val esRecurrente: Boolean = false,
    val frecuencia: String = "Ninguna", // "Semanal", "Quincenal", "Mensual"
    val fechaProximoPago: Long? = null,

    // --- NUEVOS CAMPOS PRO ---
    val diaPago1: Int? = null, // Semanal: 1(Dom) a 7(Sab). Mensual/Quincenal: 1 al 31
    val diaPago2: Int? = null  // Solo para quincenal: 1 al 31 (Ej: 15 y 30)
)


@Entity(tableName = "tarjetas_credito")
data class TarjetaCredito(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreBanco: String,
    val limiteCredito: Double,
    val diaCorte: Int,
    val diaLimitePago: Int,
    val recibirNotificaciones: Boolean = true
)
@Entity(
    tableName = "compras_msi",
    foreignKeys = [ForeignKey(
        entity = TarjetaCredito::class,
        parentColumns = ["id"],
        childColumns = ["tarjetaId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class CompraMSI(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tarjetaId: Int,
    val descripcion: String,
    val montoTotalOriginal: Double,
    val cuotasTotales: Int,
    val fechaCompra: Long,
    val cuotasPagadas: Int = 0,
    val deudor: String = "Yo"
) {
    fun montoPagoMensual(): Double = montoTotalOriginal / cuotasTotales

    fun saldoRestante(): Double = montoTotalOriginal - (montoPagoMensual() * cuotasPagadas)

    fun pagosRestantes(): Int = cuotasTotales - cuotasPagadas
}

@Entity(tableName = "gastos_diarios")
data class GastoDiario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monto: Double,
    val descripcion: String,
    val fechaGasto: Long,
    val categoria: String,
    val tarjetaId: Int? = null // <--- NULL si es efectivo/débito, ID si es crédito
)

@Entity(tableName = "deudores")
data class Deudor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String
)

@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val icono: String = "📦",
    val colorHex: Long = 0xFF00BFA5,
    val tipo: String
)
