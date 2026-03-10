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

    val tarjetas: StateFlow<List<TarjetaCredito>> = dao.obtenerTarjetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gastos: StateFlow<List<GastoDiario>> = dao.obtenerTodosLosGastos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _fechaActual = MutableStateFlow(Calendar.getInstance())
    val fechaActual = _fechaActual.asStateFlow()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val gastosFiltrados = _fechaActual.flatMapLatest { calendar ->
        val (inicio, fin) = obtenerRangoMes(calendar)
        dao.obtenerGastosPorFecha(inicio, fin)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ingresos: StateFlow<List<Ingreso>> = dao.obtenerTodosLosIngresos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deudores: StateFlow<List<Deudor>> = dao.obtenerDeudores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriasGasto: StateFlow<List<Categoria>> = dao.obtenerCategoriasPorTipo("GASTO")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriasIngreso: StateFlow<List<Categoria>> = dao.obtenerCategoriasPorTipo("INGRESO")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Categorías de GASTO por defecto
            if (dao.contarCategoriasPorTipo("GASTO") == 0) {
                val basicasGasto = listOf(
                    Triple("Comida", "🍔", 0xFFEF5350),
                    Triple("Transporte", "🚗", 0xFF42A5F5),
                    Triple("Servicios", "💡", 0xFFFFCA28),
                    Triple("Ocio", "🎮", 0xFFAB47BC),
                    Triple("Salud", "💊", 0xFF00BFA5)
                )
                basicasGasto.forEach { dao.insertarCategoria(com.fintechforge.zenimintfunds.data.Categoria(nombre = it.first, icono = it.second, colorHex = it.third, tipo = "GASTO")) }
            }

            // Categorías de INGRESO por defecto
            if (dao.contarCategoriasPorTipo("INGRESO") == 0) {
                val basicasIngreso = listOf(
                    Triple("Salario", "💰", 0xFF69F0AE),
                    Triple("Negocio", "📈", 0xFF40C4FF),
                    Triple("Regalo", "🎁", 0xFFFF80AB),
                    Triple("Inversión", "🏦", 0xFFB2FF59)
                )
                basicasIngreso.forEach { dao.insertarCategoria(com.fintechforge.zenimintfunds.data.Categoria(nombre = it.first, icono = it.second, colorHex = it.third, tipo = "INGRESO")) }
            }
        }
    }

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

    fun agregarDeudor(nombre: String) {
        viewModelScope.launch {
            val nuevoDeudor = Deudor(nombre = nombre.trim())
            dao.insertarDeudor(nuevoDeudor)
        }
    }

    fun eliminarDeudor(deudor: Deudor) {
        viewModelScope.launch {
            dao.eliminarDeudor(deudor)
        }
    }

    fun actualizarGasto(gasto: GastoDiario) = viewModelScope.launch { dao.actualizarGasto(gasto) }

    fun borrarGasto(gasto: GastoDiario) = viewModelScope.launch { dao.eliminarGasto(gasto) }

    fun actualizarCompra(compra: CompraMSI) = viewModelScope.launch { dao.actualizarCompraMSI(compra) }
    fun borrarCompra(compra: CompraMSI) = viewModelScope.launch { dao.eliminarCompraMSI(compra) }

    fun borrarTarjeta(tarjeta: TarjetaCredito) = viewModelScope.launch { dao.eliminarTarjeta(tarjeta) }

    fun agregarGasto(
        monto: Double,
        descripcion: String,
        categoria: String,
        tarjetaSeleccionadaId: Int?
    ) {
        viewModelScope.launch {
            val nuevoGasto = GastoDiario(
                monto = monto,
                descripcion = descripcion,
                categoria = categoria,
                fechaGasto = System.currentTimeMillis()
            )
            dao.insertarGasto(nuevoGasto)
        }
    }

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

    fun actualizarIngreso(ingreso: Ingreso) {
        viewModelScope.launch {
            dao.actualizarIngreso(ingreso)
        }
    }

    fun eliminarIngreso(ingreso: Ingreso) {
        viewModelScope.launch {
            dao.eliminarIngreso(ingreso)
        }
    }

    fun agregarCompraMSI(
        tarjetaId: Int,
        descripcion: String,
        montoTotal: Double,
        cuotasTotales: Int,
        deudor: String,
        cuotasPagadas: Int = 0
    ) {
        viewModelScope.launch {
            val compra = CompraMSI(
                tarjetaId = tarjetaId,
                descripcion = descripcion,
                montoTotalOriginal = montoTotal,
                cuotasTotales = cuotasTotales,
                fechaCompra = System.currentTimeMillis(),
                cuotasPagadas = cuotasPagadas,
                deudor = deudor
            )
            dao.insertarCompraMSI(compra)
        }
    }

    fun actualizarTarjeta(tarjeta: TarjetaCredito) {
        viewModelScope.launch { dao.actualizarTarjeta(tarjeta) }
    }

    fun agregarIngreso(
        monto: Double,
        descripcion: String,
        categoria: String,
        esRecurrente: Boolean = false,
        frecuencia: String = "Ninguna",
        diaPago1: Int? = null,
        diaPago2: Int? = null
    ) {
        viewModelScope.launch {
            val fechaActual = System.currentTimeMillis()

            val fechaProximo = if (esRecurrente && diaPago1 != null) {
                calcularProximaFecha(fechaActual, frecuencia, diaPago1, diaPago2)
            } else null

            val ingreso = Ingreso(
                monto = monto,
                descripcion = descripcion,
                fechaIngreso = fechaActual,
                categoria = categoria,
                esRecurrente = esRecurrente,
                frecuencia = frecuencia,
                fechaProximoPago = fechaProximo,
                diaPago1 = diaPago1,
                diaPago2 = diaPago2
            )
            dao.insertarIngreso(ingreso)
        }
    }

    private fun calcularProximaFecha(fechaBase: Long, frecuencia: String, dia1: Int, dia2: Int?): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = fechaBase }

        when (frecuencia) {
            "Semanal" -> {
                val diaActualSemana = calendar.get(Calendar.DAY_OF_WEEK)
                var diasAAgregar = dia1 - diaActualSemana
                if (diasAAgregar <= 0) diasAAgregar += 7
                calendar.add(Calendar.DAY_OF_YEAR, diasAAgregar)
            }
            "Mensual" -> {
                val diaActual = calendar.get(Calendar.DAY_OF_MONTH)
                val maxDiasEsteMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val diaObjetivo = if (dia1 > maxDiasEsteMes) maxDiasEsteMes else dia1

                if (diaActual >= diaObjetivo) {
                    calendar.add(Calendar.MONTH, 1)
                    val maxDiasOtroMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, if (dia1 > maxDiasOtroMes) maxDiasOtroMes else dia1)
                } else {
                    calendar.set(Calendar.DAY_OF_MONTH, diaObjetivo)
                }
            }
            "Quincenal" -> {
                if (dia2 == null) return calendar.timeInMillis

                val d1 = minOf(dia1, dia2)
                val d2 = maxOf(dia1, dia2)

                val diaActual = calendar.get(Calendar.DAY_OF_MONTH)
                val maxDiasEsteMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val obj1 = if (d1 > maxDiasEsteMes) maxDiasEsteMes else d1
                val obj2 = if (d2 > maxDiasEsteMes) maxDiasEsteMes else d2

                if (diaActual < obj1) {
                    calendar.set(Calendar.DAY_OF_MONTH, obj1)
                } else if (diaActual < obj2) {
                    calendar.set(Calendar.DAY_OF_MONTH, obj2)
                } else {
                    calendar.add(Calendar.MONTH, 1)
                    val maxDiasOtroMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, if (d1 > maxDiasOtroMes) maxDiasOtroMes else d1)
                }
            }
        }
        return calendar.timeInMillis
    }

    fun obtenerGastoTotalMes(anio: Int, mes: Int): StateFlow<Double?> {
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

    fun pagarMensualidadTarjeta(tarjetaId: Int, crearGastoEnDashboard: Boolean) {
        viewModelScope.launch {
            val compras = dao.obtenerComprasPorTarjeta(tarjetaId).first()

            var totalPagado = 0.0
            val nombreBanco = dao.obtenerTarjetas().first().find { it.id == tarjetaId }?.nombreBanco ?: "Tarjeta"

            compras.forEach { compra ->
                if (compra.cuotasPagadas < compra.cuotasTotales) {
                    val nuevaCuota = compra.cuotasPagadas + 1
                    val compraActualizada = compra.copy(cuotasPagadas = nuevaCuota)

                    dao.actualizarCompraMSI(compraActualizada)

                    totalPagado += (compra.montoTotalOriginal / compra.cuotasTotales)
                }
            }

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

    fun agregarCategoria(nombre: String, icono: String, colorHex: Long, tipo: String) {
        viewModelScope.launch {
            dao.insertarCategoria(Categoria(nombre = nombre, icono = icono, colorHex = colorHex, tipo = tipo))
        }
    }

    fun eliminarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            dao.eliminarCategoria(categoria)
        }
    }

    fun obtenerGastosDirectosTarjeta(tarjetaId: Int): Flow<List<GastoDiario>> {
        val inicioMes = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis
        return dao.obtenerGastosDirectosTarjeta(tarjetaId, inicioMes)
    }
}



class FinanceViewModelFactory(private val dao: FinanceDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}