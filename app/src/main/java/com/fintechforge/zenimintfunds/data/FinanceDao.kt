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

    // --- SECCIÓN 1: INGRESOS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarIngreso(ingreso: Ingreso)

    @Query("SELECT * FROM ingresos ORDER BY fechaRegistro DESC")
    fun obtenerIngresos(): Flow<List<Ingreso>>

    @Delete
    suspend fun eliminarIngreso(ingreso: Ingreso)


    // --- SECCIÓN 2: TARJETAS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTarjeta(tarjeta: TarjetaCredito)

    @Query("SELECT * FROM tarjetas_credito")
    fun obtenerTarjetas(): Flow<List<TarjetaCredito>>

    // Esta sirve para las notificaciones: traer tarjetas que aceptan alertas
    @Query("SELECT * FROM tarjetas_credito WHERE recibirNotificaciones = 1")
    fun obtenerTarjetasConAlertas(): Flow<List<TarjetaCredito>>

    @Delete
    suspend fun eliminarTarjeta(tarjeta: TarjetaCredito)


    // --- SECCIÓN 3: COMPRAS A MSI ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCompraMSI(compra: CompraMSI)

    // Usaremos esto para actualizar cuando pagues una cuota (ej: 3/12 -> 4/12)
    @Update
    suspend fun actualizarCompraMSI(compra: CompraMSI)

    @Query("SELECT * FROM compras_msi WHERE tarjetaId = :tarjetaId")
    fun obtenerComprasPorTarjeta(tarjetaId: Int): Flow<List<CompraMSI>>


    // --- SECCIÓN 4: GASTOS DIARIOS (El núcleo de tu control) ---
    @Insert
    suspend fun insertarGasto(gasto: GastoDiario)

    // Para ver el historial completo
    @Query("SELECT * FROM gastos_diarios ORDER BY fechaGasto DESC")
    fun obtenerTodosLosGastos(): Flow<List<GastoDiario>>

    // ¡ESTA ES LA CLAVE PARA TU OBJETIVO!
    // Filtra los gastos entre una fecha de inicio y fin (ej: del 1 al 30 del mes)
    @Query("SELECT * FROM gastos_diarios WHERE fechaGasto BETWEEN :inicio AND :fin ORDER BY fechaGasto DESC")
    fun obtenerGastosPorFecha(inicio: Long, fin: Long): Flow<List<GastoDiario>>

    // Suma total automática (para no usar Excel)
    @Query("SELECT SUM(monto) FROM gastos_diarios WHERE fechaGasto BETWEEN :inicio AND :fin")
    fun obtenerSumaGastosPorFecha(inicio: Long, fin: Long): Flow<Double?>
}