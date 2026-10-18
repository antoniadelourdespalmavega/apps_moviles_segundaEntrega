package cl.duoc.huertoescolar.ui.revision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.local.query.EvidenciaDetalle
import cl.duoc.huertoescolar.data.local.query.TareaDetalle
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.ResultadoValidacion
import cl.duoc.huertoescolar.domain.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Una entrega esperando revision: la tarea y su evidencia mas reciente. */
data class EntregaPorRevisar(
    val detalle: TareaDetalle,
    val evidencia: EvidenciaDetalle?
)

/** Datos editables de la pantalla (que tarea se esta rechazando y con que motivo). */
data class FormularioRevision(
    val rechazandoId: Long? = null,
    val motivo: String = "",
    val error: String? = null,
    val procesando: Boolean = false,
    val mensaje: String? = null
)

data class RevisionUiState(
    val cargando: Boolean = true,
    val entregas: List<EntregaPorRevisar> = emptyList(),
    val formulario: FormularioRevision = FormularioRevision()
)

/**
 * Revision de actividades: el docente o tallerista ve todas las entregas
 * pendientes de validacion y las valida o rechaza (con motivo obligatorio).
 */
class RevisionViewModel(private val repositorio: TareaRepository) : ViewModel() {

    private val formulario = MutableStateFlow(FormularioRevision())

    val ui: StateFlow<RevisionUiState> = combine(
        repositorio.observarTodas(),
        repositorio.observarTodasLasEvidencias(),
        formulario
    ) { tareas, evidencias, form ->
        RevisionUiState(
            cargando = false,
            entregas = tareas
                .filter { it.tarea.estado == EstadoTarea.PENDIENTE_VALIDACION }
                .map { detalle ->
                    EntregaPorRevisar(
                        detalle = detalle,
                        // Las evidencias vienen ordenadas de la mas nueva a la mas antigua.
                        evidencia = evidencias.firstOrNull { it.evidencia.tareaId == detalle.tarea.id }
                    )
                },
            formulario = form
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RevisionUiState()
    )

    /** Abre (o cierra) el campo de motivo de rechazo de una tarea. */
    fun alternarRechazo(tareaId: Long) = formulario.update {
        if (it.rechazandoId == tareaId) {
            it.copy(rechazandoId = null, motivo = "", error = null)
        } else {
            it.copy(rechazandoId = tareaId, motivo = "", error = null)
        }
    }

    fun cambiarMotivo(valor: String) = formulario.update { it.copy(motivo = valor, error = null) }

    fun validar(tareaId: Long, validadorId: Long) {
        revisar(tareaId, validadorId, ResultadoValidacion.VALIDADA, "")
    }

    fun rechazar(tareaId: Long, validadorId: Long) {
        val motivo = formulario.value.motivo
        val error = Validaciones.rechazo(motivo)[Validaciones.CAMPO_MOTIVO]
        if (error != null) {
            formulario.update { it.copy(error = error) }
            return
        }
        revisar(tareaId, validadorId, ResultadoValidacion.RECHAZADA, motivo)
    }

    private fun revisar(
        tareaId: Long,
        validadorId: Long,
        resultado: ResultadoValidacion,
        motivo: String
    ) {
        formulario.update { it.copy(procesando = true) }
        viewModelScope.launch {
            val operacion = repositorio.revisarTarea(tareaId, validadorId, resultado, motivo)
            formulario.update {
                when (operacion) {
                    is Operacion.Exito -> FormularioRevision(mensaje = operacion.mensaje)
                    is Operacion.Error -> it.copy(procesando = false, mensaje = operacion.mensaje)
                }
            }
        }
    }

    fun limpiarMensaje() = formulario.update { it.copy(mensaje = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RevisionViewModel(Contenedor.tareaRepository) }
        }
    }
}
