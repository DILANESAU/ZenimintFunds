package com.fintechforge.zenimintfunds.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fintechforge.zenimintfunds.data.AppDatabase
import com.fintechforge.zenimintfunds.data.Ingreso
import kotlinx.coroutines.flow.first
import java.util.Calendar

class FinanceWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.financeDao()

        val hoyMillis = System.currentTimeMillis()
        val hoy = Calendar.getInstance()
        val diaActual = hoy.get(Calendar.DAY_OF_MONTH)

        try {
            // ==========================================
            // LÓGICA 1: ALERTAS DE TARJETAS (Corte)
            // ==========================================
            val tarjetas = dao.obtenerTarjetas().first()
            tarjetas.forEach { tarjeta ->
                val diasParaCorte = tarjeta.diaCorte - diaActual
                // Avisamos si faltan 3, 2, 1 días o si es HOY (0)
                if (diasParaCorte in 0..3) {
                    enviarNotificacion(
                        id = tarjeta.id + 100, // Offset para no chocar IDs
                        titulo = "💳 Corte Próximo: ${tarjeta.nombreBanco}",
                        mensaje = if (diasParaCorte == 0) "¡Hoy es tu día de corte! ⚠️"
                        else "Tu tarjeta corta en $diasParaCorte días. Revisa tus gastos."
                    )
                }
            }

            // ==========================================
            // LÓGICA 2: ALERTAS DE MSI (Por terminar)
            // ==========================================
            // Buscamos compras en todas las tarjetas
            tarjetas.forEach { tarjeta ->
                val compras = dao.obtenerComprasPorTarjeta(tarjeta.id).first()
                compras.forEach { compra ->
                    val faltantes = compra.cuotasTotales - compra.cuotasPagadas
                    if (faltantes in 1..2) {
                        enviarNotificacion(
                            id = compra.id + 1000,
                            titulo = "🎉 ¡Ya casi liquidas!",
                            mensaje = "Solo faltan $faltantes meses para terminar de pagar ${compra.descripcion}"
                        )
                    }
                }
            }

            // ==========================================
            // LÓGICA 3: INGRESOS AUTOMÁTICOS
            // ==========================================
            val ingresosPendientes = dao.obtenerIngresosPendientesDeCobro(hoyMillis)

            for (ingresoViejo in ingresosPendientes) {
                // a) Creamos el nuevo registro para el historial
                val nuevoIngresoHistorico = ingresoViejo.copy(
                    id = 0, // Room genera uno nuevo
                    fechaIngreso = hoyMillis,
                    esRecurrente = false, // Este ya es un registro fijo
                    fechaProximoPago = null
                )
                dao.insertarIngreso(nuevoIngresoHistorico)

                // b) Calculamos la siguiente fecha para el registro programado
                val siguienteFecha = if (ingresoViejo.diaPago1 != null) {
                    calcularProximaFecha(hoyMillis, ingresoViejo.frecuencia, ingresoViejo.diaPago1, ingresoViejo.diaPago2)
                } else null

                // c) Actualizamos el registro "padre" para la siguiente ocasión
                val ingresoProgramadoActualizado = ingresoViejo.copy(
                    fechaProximoPago = siguienteFecha
                )
                dao.actualizarIngreso(ingresoProgramadoActualizado)

                // d) Notificamos
                enviarNotificacion(
                    id = ingresoViejo.id + 2000,
                    titulo = "💰 ¡Dinero en camino!",
                    mensaje = "Se ha registrado automáticamente: ${ingresoViejo.descripcion} por $${String.format("%,.0f", ingresoViejo.monto)}"
                )
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun calcularProximaFecha(fechaBase: Long, frecuencia: String, dia1: Int, dia2: Int?): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = fechaBase }
        val maxDiasEsteMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        when (frecuencia) {
            "Semanal" -> {
                val diaActualSemana = calendar.get(Calendar.DAY_OF_WEEK)
                var diasAAgregar = dia1 - diaActualSemana
                if (diasAAgregar <= 0) diasAAgregar += 7
                calendar.add(Calendar.DAY_OF_YEAR, diasAAgregar)
            }
            "Mensual" -> {
                val diaObjetivo = if (dia1 > maxDiasEsteMes) maxDiasEsteMes else dia1
                calendar.add(Calendar.MONTH, 1)
                val maxDiasSiguiente = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, if (dia1 > maxDiasSiguiente) maxDiasSiguiente else dia1)
            }
            "Quincenal" -> {
                if (dia2 == null) return calendar.timeInMillis + (15L * 24 * 60 * 60 * 1000)

                val d1 = minOf(dia1, dia2)
                val d2 = maxOf(dia1, dia2)
                val diaActual = calendar.get(Calendar.DAY_OF_MONTH)

                if (diaActual < d1) {
                    calendar.set(Calendar.DAY_OF_MONTH, d1)
                } else if (diaActual < d2) {
                    val d2Ajustado = if (d2 > maxDiasEsteMes) maxDiasEsteMes else d2
                    calendar.set(Calendar.DAY_OF_MONTH, d2Ajustado)
                } else {
                    calendar.add(Calendar.MONTH, 1)
                    val maxSiguiente = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, if (d1 > maxSiguiente) maxSiguiente else d1)
                }
            }
        }
        // Normalizamos a las 09:00 AM para que no se cobre a media noche
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        return calendar.timeInMillis
    }

    private fun enviarNotificacion(id: Int, titulo: String, mensaje: String) {
        val channelId = "finance_alerts"

        // Verificación de permisos para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        crearCanalNotificacion(channelId)

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000))

        NotificationManagerCompat.from(applicationContext).notify(id, builder.build())
    }

    private fun crearCanalNotificacion(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas Zenimint"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = "Notificaciones de ingresos y pagos"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}