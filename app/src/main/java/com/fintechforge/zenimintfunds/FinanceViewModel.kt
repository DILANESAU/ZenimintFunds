package com.fintechforge.zenimintfunds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fintechforge.zenimintfunds.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(private val dao: FinanceDao) : ViewModel() {

    // 1. Lista de Tarjetas (Se actualiza sola si agregas una)
    val tarjetas: StateFlow<List<TarjetaCredito>> = dao.obtenerTarjetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Lista de Gastos (Historial completo)
    val gastos: StateFlow<List<GastoDiario>> = dao.obtenerTodosLosGastos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Lista de Ingresos
    val ingresos: StateFlow<List<Ingreso>> = dao.obtenerIngresos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- ACCIONES (Lo que el usuario puede "hacer") ---

    // Función para agregar un GASTO DIARIO
    fun agregarGasto(monto: Double, descripcion: String, categoria: String) {
        // Lanzamos una "corrutina" (hilo secundario) para no trabar la app
        viewModelScope.launch {
            val nuevoGasto = GastoDiario(
                monto = monto,
                descripcion = descripcion,
                categoria = categoria,
                fechaGasto = System.currentTimeMillis() // Fecha de hoy
            )
            dao.insertarGasto(nuevoGasto)
        }
    }

    // Función para agregar una TARJETA
    fun agregarTarjeta(banco: String, limite: Double, corte: Int, pago: Int) {
        viewModelScope.launch {
            val nuevaTarjeta = TarjetaCredito(
                nombreBanco = banco,
                limiteCredito = limite,
                diaCorte = corte,
                diaLimitePago = pago
            )
            dao.insertarTarjeta(nuevaTarjeta)
        }
    }

    // Función para agregar una COMPRA A MSI
    fun agregarCompraMSI(tarjetaId: Int, desc: String, total: Double, cuotas: Int) {
        viewModelScope.launch {
            val nuevaCompra = CompraMSI(
                tarjetaId = tarjetaId,
                descripcion = desc,
                montoTotalOriginal = total,
                cuotasTotales = cuotas,
                fechaCompra = System.currentTimeMillis()
            )
            dao.insertarCompraMSI(nuevaCompra)
        }
    }

    // Función para agregar INGRESO
    fun agregarIngreso(monto: Double, fuente: String, frecuencia: FrecuenciaPago) {
        viewModelScope.launch {
            val nuevoIngreso = Ingreso(
                monto = monto,
                fuente = fuente,
                frecuencia = frecuencia,
                fechaRegistro = System.currentTimeMillis()
            )
            dao.insertarIngreso(nuevoIngreso)
        }
    }

    // --- CÁLCULOS ESPECIALES ---

    // Calcular cuánto has gastado este mes (para tu objetivo principal)
    fun obtenerGastoTotalMes(anio: Int, mes: Int): StateFlow<Double?> {
        // Lógica para obtener el primer y último milisegundo del mes
        val calendar = Calendar.getInstance()
        calendar.set(anio, mes, 1, 0, 0, 0)
        val inicio = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val fin = calendar.timeInMillis

        return dao.obtenerSumaGastosPorFecha(inicio, fin)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }
}

// --- FACTORY (La Fábrica) ---
// Esto es necesario porque nuestro ViewModel tiene un parámetro (dao)
// Android necesita saber cómo construirlo.
class FinanceViewModelFactory(private val dao: FinanceDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}