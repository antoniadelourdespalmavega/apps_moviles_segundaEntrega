package cl.duoc.huertoescolar.ui.tareas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.local.query.TareaDetalle
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.Alertas
import cl.duoc.huertoescolar.domain.CategoriaTarea
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Filtro de fecha del listado de tareas. */
enum class FiltroFecha(val etiqueta: String) {
    TODAS("Todas las fechas"),
    HOY("Programadas para hoy"),
    SEMANA("Proximos 7 dias")
}

data class FiltrosTareas(
    val categoria: CategoriaTarea? = null,
    val responsableId: Long? = null,
    val fecha: FiltroFecha = FiltroFecha.TODAS
)

data class TareasUiState(
    val cargando: Boolean = true,
    val tareas: List<TareaDetalle> = emptyList(),
    val estudiantes: List<Usuario> = emptyList(),
    val filtros: FiltrosTareas = FiltrosTareas(),
    /** Total por categoria: los mismos numeros que muestra el panel principal. */
    val conteo: Map<CategoriaTarea, Int> = emptyMap(),
    val total: Int = 0,
    val mensaje: String? = null,
    val esAdministrador: Boolean = false
)

class TareasViewModel(
    private val tareaRepository: TareaRepository,
    sesionRepository: SesionRepository
) : ViewModel() {

    private val usuario = MutableStateFlow<Usuario?>(null)
    private val filtros = MutableStateFlow(FiltrosTareas())
    private val mensaje = MutableStateFlow<String?>(null)

    val ui: StateFlow<TareasUiState> = combine(
        usuario,
        tareaRepository.observarTodas(),
        sesionRepository.observarEstudiantes(),
        filtros,
        mensaje
    ) { usuarioActual, tareas, estudiantes, filtrosActuales, aviso ->
        val esAdmin = usuarioActual?.rol == Rol.ADMINISTRADOR
        val hoy = Fechas.hoy()

        // Un estudiante solo puede ver las tareas que tiene asignadas.
        val visibles = if (esAdmin) {
            tareas
        } else {
            tareas.filter { it.tarea.responsableId == (usuarioActual?.id ?: -1L) }
        }

        val filtradas = visibles.filter { detalle ->
            val porCategoria = filtrosActuales.categoria == null ||
                Alertas.categoria(detalle, hoy) == filtrosActuales.categoria
            val porResponsable = filtrosActuales.responsableId == null ||
                detalle.tarea.responsableId == filtrosActuales.responsableId
            val dias = Fechas.diasEntre(hoy, detalle.tarea.fechaProgramada)
            val porFecha = when (filtrosActuales.fecha) {
                FiltroFecha.TODAS -> true
                FiltroFecha.HOY -> dias == 0
                FiltroFecha.SEMANA -> dias in 0..7
            }
            porCategoria && porResponsable && porFecha
        }

        TareasUiState(
            cargando = false,
            tareas = filtradas,
            estudiantes = estudiantes,
            filtros = filtrosActuales,
            conteo = Alertas.contarPorCategoria(visibles, hoy),
            total = visibles.size,
            mensaje = aviso,
            esAdministrador = esAdmin
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TareasUiState()
    )

    fun establecerUsuario(usuarioActual: Usuario) {
        usuario.value = usuarioActual
    }

    fun filtrarPorCategoria(categoria: CategoriaTarea?) {
        filtros.value = filtros.value.copy(categoria = categoria)
    }

    private var categoriaInicialAplicada = false

    /** Aplica (una sola vez) el filtro con que se abrio la pantalla desde el panel. */
    fun aplicarCategoriaInicial(categoria: CategoriaTarea?) {
        if (categoriaInicialAplicada) return
        categoriaInicialAplicada = true
        filtros.value = FiltrosTareas(categoria = categoria)
    }

    fun filtrarPorResponsable(responsableId: Long?) {
        filtros.value = filtros.value.copy(responsableId = responsableId)
    }

    fun filtrarPorFecha(fecha: FiltroFecha) {
        filtros.value = filtros.value.copy(fecha = fecha)
    }

    fun limpiarFiltros() {
        filtros.value = FiltrosTareas()
    }

    fun eliminarTarea(tareaId: Long) {
        val usuarioActual = usuario.value ?: return
        viewModelScope.launch {
            when (val resultado = tareaRepository.eliminarTarea(tareaId, usuarioActual.id)) {
                is Operacion.Exito -> mensaje.value = resultado.mensaje
                is Operacion.Error -> mensaje.value = resultado.mensaje
            }
        }
    }

    fun limpiarMensaje() {
        mensaje.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TareasViewModel(Contenedor.tareaRepository, Contenedor.sesionRepository)
            }
        }
    }
}
