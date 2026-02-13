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
import com.fintechforge.zenimintfunds.R // Asegúrate de que este R sea el de tu paquete
import com.fintechforge.zenimintfunds.data.AppDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar

class FinanceWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Obtener la base de datos
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.financeDao()

        // 2. Obtener tarjetas y MSI (usamos .first() para obtener el dato actual del Flow)
        val tarjetas = dao.obtenerTarjetas().first()

        // 3. Revisar fechas
        val hoy = Calendar.getInstance()
        val diaActual = hoy.get(Calendar.DAY_OF_MONTH)

        tarjetas.forEach { tarjeta ->
            // --- LÓGICA 1: ALERTA DE CORTE ---
            // Si faltan 3 días o menos para el corte
            val diasParaCorte = tarjeta.diaCorte - diaActual
            if (diasParaCorte in 0..3) {
                enviarNotificacion(
                    id = tarjeta.id,
                    titulo = "💳 Corte Próximo: ${tarjeta.nombreBanco}",
                    mensaje = "Tu tarjeta corta en $diasParaCorte días (Día ${tarjeta.diaCorte}). Revisa tus gastos."
                )
            }

            // --- LÓGICA 2: ALERTA DE MSI POR TERMINAR ---
            // Obtenemos las compras de esta tarjeta
            val compras = dao.obtenerComprasPorTarjeta(tarjeta.id).first()
            compras.forEach { compra ->
                val faltantes = compra.cuotasTotales - compra.cuotasPagadas
                if (faltantes in 1..2) {
                    enviarNotificacion(
                        id = compra.id + 1000, // ID diferente para no sobreescribir
                        titulo = "🎉 ¡Ya casi terminas!",
                        mensaje = "Solo faltan $faltantes pagos para liquidar: ${compra.descripcion}"
                    )
                }
            }
        }

        return Result.success()
    }

    private fun enviarNotificacion(id: Int, titulo: String, mensaje: String) {
        // Verificar permiso en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return // No tenemos permiso, no hacemos nada
            }
        }

        val channelId = "finance_alerts"
        crearCanalNotificacion(channelId)

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icono por defecto (puedes poner uno tuyo luego)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(applicationContext).notify(id, builder.build())
    }

    private fun crearCanalNotificacion(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas Financieras"
            val descriptionText = "Avisos de corte y pagos"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}