package com.fintechforge.zenimintfunds.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarIngreso(ingreso: Ingreso)

    // --- LAS QUE FALTABAN PARA EL DASHBOARD ---
    @Query("SELECT * FROM ingresos ORDER BY fechaIngreso DESC")
    fun obtenerTodosLosIngresos(): Flow<List<Ingreso>>

    @Query("SELECT * FROM ingresos WHERE fechaIngreso BETWEEN :inicio AND :fin ORDER BY fechaIngreso DESC")
    fun obtenerIngresosPorFecha(inicio: Long, fin: Long): Flow<List<Ingreso>>
    @Query("SELECT * FROM ingresos WHERE esRecurrente = 1 AND fechaProximoPago <= :fechaActual")
    suspend fun obtenerIngresosPendientesDeCobro(fechaActual: Long): List<Ingreso>

    @Delete
    suspend fun eliminarIngreso(ingreso: Ingreso)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTarjeta(tarjeta: TarjetaCredito)

    @Query("SELECT * FROM tarjetas_credito")
    fun obtenerTarjetas(): Flow<List<TarjetaCredito>>

    @Query("SELECT * FROM tarjetas_credito WHERE recibirNotificaciones = 1")
    fun obtenerTarjetasConAlertas(): Flow<List<TarjetaCredito>>

    @Delete
    suspend fun eliminarTarjeta(tarjeta: TarjetaCredito)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCompraMSI(compra: CompraMSI)

    @Update
    suspend fun actualizarCompraMSI(compra: CompraMSI)

    @Query("SELECT * FROM compras_msi WHERE tarjetaId = :tarjetaId")
    fun obtenerComprasPorTarjeta(tarjetaId: Int): Flow<List<CompraMSI>>

    @Insert
    suspend fun insertarGasto(gasto: GastoDiario)

    @Query("SELECT * FROM gastos_diarios ORDER BY fechaGasto DESC")
    fun obtenerTodosLosGastos(): Flow<List<GastoDiario>>

    @Query("SELECT * FROM gastos_diarios WHERE fechaGasto BETWEEN :inicio AND :fin ORDER BY fechaGasto DESC")
    fun obtenerGastosPorFecha(inicio: Long, fin: Long): Flow<List<GastoDiario>>

    @Query("SELECT SUM(monto) FROM gastos_diarios WHERE fechaGasto BETWEEN :inicio AND :fin")
    fun obtenerSumaGastosPorFecha(inicio: Long, fin: Long): Flow<Double?>

    @Update suspend fun actualizarGasto(gasto: GastoDiario)
    @Delete suspend fun eliminarGasto(gasto: GastoDiario)
    @Update suspend fun actualizarIngreso(ingreso: Ingreso)
    @Update suspend fun actualizarTarjeta(tarjeta: TarjetaCredito)
    @Delete suspend fun eliminarCompraMSI(compra: CompraMSI)
    // --- GESTIÓN DE DEUDORES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarDeudor(deudor: Deudor)

    @Query("SELECT * FROM deudores ORDER BY nombre ASC")
    fun obtenerDeudores(): Flow<List<Deudor>>

    @Delete
    suspend fun eliminarDeudor(deudor: Deudor)

    // --- GESTIÓN DE CATEGORÍAS (FILTRADAS) ---
    @Query("SELECT * FROM categorias WHERE tipo = :tipo ORDER BY nombre ASC")
    fun obtenerCategoriasPorTipo(tipo: String): Flow<List<Categoria>>

    @Query("SELECT COUNT(*) FROM categorias WHERE tipo = :tipo")
    suspend fun contarCategoriasPorTipo(tipo: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCategoria(categoria: Categoria)

    @Delete
    suspend fun eliminarCategoria(categoria: Categoria)

    @Query("SELECT * FROM gastos_diarios WHERE tarjetaId = :tarjetaId AND fechaGasto >= :inicioMes")
    fun obtenerGastosDirectosTarjeta(tarjetaId: Int, inicioMes: Long): Flow<List<GastoDiario>>
}