package cl.duoc.huertoescolar.ui.tareas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.local.query.EvidenciaDetalle
import cl.duoc.huertoescolar.data.local.query.TareaDetalle
import cl.duoc.huertoescolar.data.local.query.ValidacionDetalle
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.ResultadoValidacion
import cl.duoc.huertoescolar.domain.Validaciones
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TareaDetalleUiState(
    val cargando: Boolean = true,
    val detalle: TareaDetalle? = null,
    val evidencias: List<EvidenciaDetalle> = emptyList(),
    val validaciones: List<ValidacionDetalle> = emptyList(),
    val comentario: String = "",
    val rutaEvidencia: String? = null,
    val motivoRechazo: String = "",
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    val procesando: Boolean = false
)

class TareaDetalleViewModel(private val repositorio: TareaRepository) : ViewModel() {

    private val tareaId = MutableStateFlow(0L)
    private val formulario = MutableStateFlow(FormularioEntrega())

    private data class FormularioEntrega(
        val comentario: String = "",
        val rutaEvidencia: String? = null,
        val motivoRechazo: String = "",
        val errores: Map<String, String> = emptyMap(),
        val mensaje: String? = null,
        val procesando: Boolean = false
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val ui: StateFlow<TareaDetalleUiState> = tareaId
        .flatMapLatest { id ->
            if (id <= 0L) {
                flowOf(TareaDetalleUiState(cargando = false))
            } else {
                combine(
                    repositorio.observarDetalle(id),
                    repositorio.observarEvidencias(id),
                    repositorio.observarValidacionesDeTarea(id),
                    formulario
                ) { detalle, evidencias, validaciones, form ->
                    TareaDetalleUiState(
                        cargando = false,
                        detalle = detalle,
                        evidencias = evidencias,
                        validaciones = validaciones,
                        comentario = form.comentario,
                        rutaEvidencia = form.rutaEvidencia,
                        motivoRechazo = form.motivoRechazo,
                        errores = form.errores,
                        mensaje = form.mensaje,
                        procesando = form.procesando
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TareaDetalleUiState()
        )

    fun establecerTarea(id: Long) {
        if (tareaId.value != id) {
            tareaId.value = id
            formulario.value = FormularioEntrega()
        }
    }

    fun cambiarComentario(valor: String) {
        formulario.value = formulario.value.copy(
            comentario = valor,
            errores = formulario.value.errores - Validaciones.CAMPO_COMENTARIO
        )
    }

    fun cambiarEvidencia(ruta: String?) {
        formulario.value = formulario.value.copy(
            rutaEvidencia = ruta,
            errores = formulario.value.errores - Validaciones.CAMPO_EVIDENCIA
        )
    }

    fun cambiarMotivo(valor: String) {
        formulario.value = formulario.value.copy(
            motivoRechazo = valor,
            errores = formulario.value.errores - Validaciones.CAMPO_MOTIVO
        )
    }

    fun avisar(texto: String) {
        formulario.value = formulario.value.copy(mensaje = texto)
    }

    fun iniciar(usuarioId: Long) {
        viewModelScope.launch {
            procesar(repositorio.iniciarTarea(tareaId.value, usuarioId))
        }
    }

    fun entregar(usuarioId: Long) {
        val form = formulario.value
        val errores = Validaciones.entregaDeTarea(form.comentario, form.rutaEvidencia)
        if (errores.isNotEmpty()) {
            formulario.value = form.copy(
                errores = errores,
                mensaje = "Completa el comentario y adjunta una fotografia"
            )
            return
        }
        formulario.value = form.copy(procesando = true, errores = emptyMap())
        viewModelScope.launch {
            val resultado = repositorio.entregarTarea(
                tareaId = tareaId.value,
                usuarioId = usuarioId,
                comentario = form.comentario,
                rutaFoto = form.rutaEvidencia ?: ""
            )
            procesar(resultado, limpiarFormulario = resultado is Operacion.Exito)
        }
    }

    fun validar(validadorId: Long) {
        formulario.value = formulario.value.copy(procesando = true)
        viewModelScope.launch {
            procesar(
                repositorio.revisarTarea(
                    tareaId = tareaId.value,
                    validadorId = validadorId,
                    resultado = ResultadoValidacion.VALIDADA,
                    motivo = "Actividad aprobada"
                )
            )
        }
    }

    fun rechazar(validadorId: Long) {
        val form = formulario.value
        val errores = Validaciones.rechazo(form.motivoRechazo)
        if (errores.isNotEmpty()) {
            formulario.value = form.copy(
                errores = errores,
                mensaje = "Debes indicar el motivo del rechazo"
            )
            return
        }
        formulario.value = form.copy(procesando = true, errores = emptyMap())
        viewModelScope.launch {
            val resultado = repositorio.revisarTarea(
                tareaId = tareaId.value,
                validadorId = validadorId,
                resultado = ResultadoValidacion.RECHAZADA,
                motivo = form.motivoRechazo
            )
            procesar(resultado, limpiarFormulario = resultado is Operacion.Exito)
        }
    }

    private fun procesar(resultado: Operacion, limpiarFormulario: Boolean = false) {
        val texto = when (resultado) {
            is Operacion.Exito -> resultado.mensaje
            is Operacion.Error -> resultado.mensaje
        }
        formulario.value = if (limpiarFormulario) {
            FormularioEntrega(mensaje = texto)
        } else {
            formulario.value.copy(procesando = false, mensaje = texto)
        }
    }

    fun limpiarMensaje() {
        formulario.value = formulario.value.copy(mensaje = null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { TareaDetalleViewModel(Contenedor.tareaRepository) }
        }
    }
}
