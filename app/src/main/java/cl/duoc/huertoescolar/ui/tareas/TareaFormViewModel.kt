package cl.duoc.huertoescolar.ui.tareas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.TipoTarea
import cl.duoc.huertoescolar.domain.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TareaFormUiState(
    val id: Long = 0,
    val tipo: TipoTarea? = null,
    val descripcion: String = "",
    val instrucciones: String = "",
    val sectorId: Long? = null,
    val cultivoId: Long? = null,
    val responsableId: Long? = null,
    val fechaProgramada: String = "",
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    val guardando: Boolean = false,
    val guardado: Boolean = false
) {
    val esEdicion: Boolean get() = id != 0L
}

class TareaFormViewModel(
    private val tareaRepository: TareaRepository,
    huertoRepository: HuertoRepository,
    sesionRepository: SesionRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(TareaFormUiState())
    val ui: StateFlow<TareaFormUiState> = _ui.asStateFlow()

    val sectores: StateFlow<List<Sector>> = huertoRepository.observarSectoresSimples()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cultivos: StateFlow<List<Cultivo>> = huertoRepository.observarCultivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val estudiantes: StateFlow<List<Usuario>> = sesionRepository.observarEstudiantes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cargar(tareaId: Long) {
        if (tareaId == 0L || _ui.value.id == tareaId) return
        viewModelScope.launch {
            val tarea = tareaRepository.porId(tareaId) ?: return@launch
            _ui.value = TareaFormUiState(
                id = tarea.id,
                tipo = tarea.tipo,
                descripcion = tarea.descripcion,
                instrucciones = tarea.instrucciones,
                sectorId = tarea.sectorId,
                cultivoId = tarea.cultivoId,
                responsableId = tarea.responsableId,
                fechaProgramada = Fechas.formatear(tarea.fechaProgramada)
            )
        }
    }

    fun cambiarTipo(valor: TipoTarea) = _ui.update {
        it.copy(tipo = valor, errores = it.errores - Validaciones.CAMPO_TIPO)
    }

    fun cambiarDescripcion(valor: String) = _ui.update {
        it.copy(descripcion = valor, errores = it.errores - Validaciones.CAMPO_DESCRIPCION)
    }

    fun cambiarInstrucciones(valor: String) = _ui.update {
        it.copy(instrucciones = valor, errores = it.errores - Validaciones.CAMPO_INSTRUCCIONES)
    }

    fun cambiarSector(valor: Long) = _ui.update {
        // Al cambiar de sector se limpia el cultivo, porque pertenece a otro sector.
        it.copy(
            sectorId = valor,
            cultivoId = null,
            errores = it.errores - Validaciones.CAMPO_SECTOR
        )
    }

    fun cambiarCultivo(valor: Long?) = _ui.update { it.copy(cultivoId = valor) }

    fun cambiarResponsable(valor: Long) = _ui.update {
        it.copy(responsableId = valor, errores = it.errores - Validaciones.CAMPO_RESPONSABLE)
    }

    fun cambiarFecha(valor: String) = _ui.update {
        it.copy(fechaProgramada = valor, errores = it.errores - Validaciones.CAMPO_FECHA_PROGRAMADA)
    }

    fun guardar(usuarioId: Long) {
        val estado = _ui.value
        val errores = Validaciones.tarea(
            descripcion = estado.descripcion,
            instrucciones = estado.instrucciones,
            tipo = estado.tipo,
            sectorId = estado.sectorId,
            responsableId = estado.responsableId,
            fechaProgramada = estado.fechaProgramada,
            esNueva = !estado.esEdicion
        )
        if (errores.isNotEmpty()) {
            _ui.update { it.copy(errores = errores, mensaje = "Revisa los campos marcados") }
            return
        }
        _ui.update { it.copy(guardando = true, errores = emptyMap()) }
        viewModelScope.launch {
            val resultado = tareaRepository.guardarTarea(
                id = estado.id,
                tipo = estado.tipo ?: TipoTarea.OTRA,
                descripcion = estado.descripcion,
                instrucciones = estado.instrucciones,
                sectorId = estado.sectorId ?: 0L,
                cultivoId = estado.cultivoId,
                responsableId = estado.responsableId ?: 0L,
                fechaProgramada = Fechas.parsear(estado.fechaProgramada) ?: Fechas.hoy(),
                usuarioQueEditaId = usuarioId
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

    fun limpiarMensaje() = _ui.update { it.copy(mensaje = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TareaFormViewModel(
                    Contenedor.tareaRepository,
                    Contenedor.huertoRepository,
                    Contenedor.sesionRepository
                )
            }
        }
    }
}
