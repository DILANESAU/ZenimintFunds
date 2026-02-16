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

    @Query("SELECT * FROM ingresos ORDER BY fechaRegistro DESC")
    fun obtenerIngresos(): Flow<List<Ingreso>>

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
}