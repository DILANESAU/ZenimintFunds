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
    val fuente: String,
    val frecuencia: FrecuenciaPago,
    val fechaRegistro: Long
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
    val categoria: String,
    val fechaGasto: Long
)