package com.fintechforge.zenimintfunds.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fintechforge.zenimintfunds.utils.NotificationHelper

class ReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.sendNotification(
            "¿Día pesado? ☕",
            "Ya casi termina el día. ¿Revisamos si capturaste todos tus gastos?",
            99 // ID único para el recordatorio
        )
        return Result.success()
    }
}