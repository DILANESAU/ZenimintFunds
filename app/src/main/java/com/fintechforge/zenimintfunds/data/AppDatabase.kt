package com.fintechforge.zenimintfunds.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Aquí listamos TODAS las tablas que creamos en el paso 1
@Database(
    entities = [
        Ingreso::class,
        TarjetaCredito::class,
        CompraMSI::class,
        GastoDiario::class
    ],
    version = 1, // Si en el futuro cambias la estructura, subes este número
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Aquí conectamos el DAO que hicimos en el paso 2
    abstract fun financeDao(): FinanceDao

    // Esto es un patrón de diseño llamado "Singleton"
    // Sirve para asegurar que solo exista UNA conexión a la base de datos a la vez
    // (Abrir muchas conexiones hace lento el celular)
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Si ya existe, la devolvemos
            return INSTANCE ?: synchronized(this) {
                // Si no existe, la creamos
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finanzas_personales_db" // Nombre del archivo en el celular
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}