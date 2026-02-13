package com.fintechforge.zenimintfunds.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow // Importante
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest // Importante para la magia reactiva
import com.fintechforge.zenimintfunds.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    private val _fechaActual = MutableStateFlow(Calendar.getInstance())
    val fechaActual = _fechaActual.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val gastosFiltrados = _fechaActual.flatMapLatest { calendar ->
        val (inicio, fin) = obtenerRangoMes(calendar)
        dao.obtenerGastosPorFecha(inicio, fin)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Lista de Ingresos
    val ingresos: StateFlow<List<Ingreso>> = dao.obtenerIngresos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cambiarMes(mesesASumar: Int) {
        val nuevoCalendario = _fechaActual.value.clone() as Calendar
        nuevoCalendario.add(Calendar.MONTH, mesesASumar)
        _fechaActual.value = nuevoCalendario
    }
    private fun obtenerRangoMes(fecha: Calendar): Pair<Long, Long> {
        val inicio = fecha.clone() as Calendar
        inicio.set(Calendar.DAY_OF_MONTH, 1)
        inicio.set(Calendar.HOUR_OF_DAY, 0)
        inicio.set(Calendar.MINUTE, 0)
        inicio.set(Calendar.SECOND, 0)

        val fin = fecha.clone() as Calendar
        fin.set(Calendar.DAY_OF_MONTH, fin.getActualMaximum(Calendar.DAY_OF_MONTH))
        fin.set(Calendar.HOUR_OF_DAY, 23)
        fin.set(Calendar.MINUTE, 59)
        fin.set(Calendar.SECOND, 59)

        return Pair(inicio.timeInMillis, fin.timeInMillis)
    }
// --- FUNCIONES DE EDICIÓN Y BORRADO ---

    // Gastos
    fun actualizarGasto(gasto: GastoDiario) = viewModelScope.launch { dao.actualizarGasto(gasto) }
    fun borrarGasto(gasto: GastoDiario) = viewModelScope.launch { dao.eliminarGasto(gasto) }

    // MSI
    fun actualizarCompra(compra: CompraMSI) = viewModelScope.launch { dao.actualizarCompraMSI(compra) }
    fun borrarCompra(compra: CompraMSI) = viewModelScope.launch { dao.eliminarCompraMSI(compra) }

    // Tarjetas
    fun borrarTarjeta(tarjeta: TarjetaCredito) = viewModelScope.launch { dao.eliminarTarjeta(tarjeta) }

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
    fun agregarCompraMSI(tarjetaId: Int, desc: String, total: Double, cuotas: Int, deudor: String) {
        viewModelScope.launch {
            val nuevaCompra = CompraMSI(
                tarjetaId = tarjetaId,
                descripcion = desc,
                montoTotalOriginal = total,
                cuotasTotales = cuotas,
                fechaCompra = System.currentTimeMillis(),
                deudor = deudor
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

    fun obtenerComprasPorTarjeta(tarjetaId: Int): Flow<List<CompraMSI>> {
        return dao.obtenerComprasPorTarjeta(tarjetaId)
    }
    // --- AUTOMATIZACIÓN DE PAGOS ---

    fun pagarMensualidadTarjeta(tarjetaId: Int, crearGastoEnDashboard: Boolean) {
        viewModelScope.launch {
            // 1. Obtenemos las compras de esa tarjeta
            val compras = dao.obtenerComprasPorTarjeta(tarjetaId).first()

            var totalPagado = 0.0
            val nombreBanco = dao.obtenerTarjetas().first().find { it.id == tarjetaId }?.nombreBanco ?: "Tarjeta"

            compras.forEach { compra ->
                // Solo avanzamos si no ha terminado de pagar
                if (compra.cuotasPagadas < compra.cuotasTotales) {
                    val nuevaCuota = compra.cuotasPagadas + 1
                    val compraActualizada = compra.copy(cuotasPagadas = nuevaCuota)

                    // Actualizamos en BD
                    dao.actualizarCompraMSI(compraActualizada)

                    // Sumamos al total para el registro
                    totalPagado += (compra.montoTotalOriginal / compra.cuotasTotales)
                }
            }

            // 2. Opcional: Crear el registro en el Dashboard para que se reste de tu dinero
            if (crearGastoEnDashboard && totalPagado > 0) {
                val gastoDelPago = GastoDiario(
                    monto = totalPagado,
                    descripcion = "Pago Tarjeta: $nombreBanco",
                    categoria = "Deudas / Crédito",
                    fechaGasto = System.currentTimeMillis()
                )
                dao.insertarGasto(gastoDelPago)
            }
        }
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