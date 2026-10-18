package cl.duoc.huertoescolar.ui.cultivos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.domain.EstadoCultivo
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CultivoFormUiState(
    val id: Long = 0,
    val nombre: String = "",
    val descripcion: String = "",
    val sectorId: Long? = null,
    val fechaSiembra: String = "",
    val fechaTrasplante: String = "",
    val fechaCosecha: String = "",
    val frecuenciaRiego: String = "",
    val estado: EstadoCultivo = EstadoCultivo.PLANIFICADO,
    val observaciones: String = "",
    val rutaFoto: String? = null,
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    val guardando: Boolean = false,
    val guardado: Boolean = false
) {
    val esEdicion: Boolean get() = id != 0L
}

class CultivoFormViewModel(private val repositorio: HuertoRepository) : ViewModel() {

    private val _ui = MutableStateFlow(CultivoFormUiState())
    val ui: StateFlow<CultivoFormUiState> = _ui.asStateFlow()

    val sectores: StateFlow<List<Sector>> = repositorio.observarSectoresSimples()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun preparar(sectorId: Long, cultivoId: Long) {
        if (cultivoId == 0L) {
            // Cultivo nuevo: la fecha de siembra se registra sola con la fecha de hoy
            // (el formulario ya no la pide) y no tiene trasplante.
            if (_ui.value.fechaSiembra.isEmpty()) {
                _ui.update {
                    it.copy(
                        sectorId = if (sectorId > 0) sectorId else null,
                        fechaSiembra = Fechas.formatear(Fechas.hoy())
                    )
                }
            }
            return
        }
        if (_ui.value.id == cultivoId) return
        viewModelScope.launch {
            val cultivo = repositorio.cultivoPorId(cultivoId) ?: return@launch
            _ui.value = CultivoFormUiState(
                id = cultivo.id,
                nombre = cultivo.nombre,
                descripcion = cultivo.descripcion,
                sectorId = cultivo.sectorId,
                fechaSiembra = Fechas.formatear(cultivo.fechaSiembra),
                fechaTrasplante = if (cultivo.fechaTrasplante == null) {
                    ""
                } else {
                    Fechas.formatear(cultivo.fechaTrasplante)
                },
                fechaCosecha = Fechas.formatear(cultivo.fechaCosechaEstimada),
                frecuenciaRiego = cultivo.frecuenciaRiegoDias.toString(),
                estado = cultivo.estado,
                observaciones = cultivo.observaciones,
                rutaFoto = cultivo.rutaFoto
            )
        }
    }

    fun cambiarNombre(valor: String) = _ui.update {
        it.copy(nombre = valor, errores = it.errores - Validaciones.CAMPO_NOMBRE)
    }

    fun cambiarDescripcion(valor: String) = _ui.update {
        it.copy(descripcion = valor, errores = it.errores - Validaciones.CAMPO_DESCRIPCION)
    }

    fun cambiarSector(valor: Long) = _ui.update {
        it.copy(sectorId = valor, errores = it.errores - Validaciones.CAMPO_SECTOR)
    }

    fun cambiarCosecha(valor: String) = _ui.update {
        it.copy(fechaCosecha = valor, errores = it.errores - Validaciones.CAMPO_COSECHA)
    }

    fun cambiarRiego(valor: String) = _ui.update {
        it.copy(frecuenciaRiego = valor, errores = it.errores - Validaciones.CAMPO_RIEGO)
    }

    fun cambiarEstado(valor: EstadoCultivo) = _ui.update { it.copy(estado = valor) }

    fun cambiarObservaciones(valor: String) = _ui.update { it.copy(observaciones = valor) }

    fun cambiarFoto(ruta: String?) = _ui.update { it.copy(rutaFoto = ruta) }

    fun avisar(texto: String) = _ui.update { it.copy(mensaje = texto) }

    fun guardar() {
        val estado = _ui.value
        val errores = Validaciones.cultivo(
            nombre = estado.nombre,
            descripcion = estado.descripcion,
            sectorId = estado.sectorId,
            fechaSiembra = estado.fechaSiembra,
            // El trasplante ya no se edita en el formulario: se conserva el guardado.
            fechaTrasplante = "",
            fechaCosecha = estado.fechaCosecha,
            frecuenciaRiego = estado.frecuenciaRiego
        )
        if (errores.isNotEmpty()) {
            _ui.update { it.copy(errores = errores, mensaje = "Revisa los campos marcados") }
            return
        }
        _ui.update { it.copy(guardando = true, errores = emptyMap()) }
        viewModelScope.launch {
            val resultado = repositorio.guardarCultivo(
                id = estado.id,
                nombre = estado.nombre,
                descripcion = estado.descripcion,
                sectorId = estado.sectorId ?: 0L,
                fechaSiembra = Fechas.parsear(estado.fechaSiembra) ?: 0L,
                fechaTrasplante = Fechas.parsear(estado.fechaTrasplante),
                fechaCosechaEstimada = Fechas.parsear(estado.fechaCosecha) ?: 0L,
                frecuenciaRiegoDias = estado.frecuenciaRiego.trim().toIntOrNull() ?: 1,
                estado = estado.estado,
                observaciones = estado.observaciones,
                rutaFoto = estado.rutaFoto
            )
            when (resultado) {
                is Operacion.Exito -> _ui.update {
                    it.copy(guardando = false, guardado = true, mensaje = resultado.mensaje)
                }
                is Operacion.Error -> _ui.update {
                    it.copy(guardando = false, mensaje = resultado.mensaje)
                }
            }
        }
    }

    /** Elimina el cultivo en edicion; no se permite si tiene tareas registradas. */
    fun eliminar(alEliminado: (String) -> Unit) {
        viewModelScope.launch {
            when (val resultado = repositorio.eliminarCultivo(_ui.value.id)) {
                is Operacion.Exito -> alEliminado(resultado.mensaje)
                is Operacion.Error -> _ui.update { it.copy(mensaje = resultado.mensaje) }
            }
        }
    }

    fun limpiarMensaje() = _ui.update { it.copy(mensaje = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CultivoFormViewModel(Contenedor.huertoRepository) }
        }
    }
}
