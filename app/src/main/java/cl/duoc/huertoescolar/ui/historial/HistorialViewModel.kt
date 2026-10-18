package cl.duoc.huertoescolar.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.repository.ReporteRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Tipo de registro dentro del historial cronologico. */
enum class TipoRegistro(val etiqueta: String) {
    TAREA("Tarea asignada"),
    EVIDENCIA("Actividad entregada"),
    VALIDACION("Revision"),
    REPORTE("Problema reportado")
}

data class RegistroHistorial(
    val id: String,
    val tipo: TipoRegistro,
    val titulo: String,
    val detalle: String,
    val autor: String,
    val fecha: Long
)

/** Periodo del filtro de fecha del historial. */
enum class PeriodoHistorial(val etiqueta: String) {
    TODO("Todo el historial"),
    HOY("Solo hoy"),
    SEMANA("Ultimos 7 dias")
}

data class FiltrosHistorial(
    val tipo: TipoRegistro? = null,
    val periodo: PeriodoHistorial = PeriodoHistorial.TODO,
    val autor: String? = null
) {
    val activos: Boolean get() = tipo != null || periodo != PeriodoHistorial.TODO || autor != null
}

data class HistorialUiState(
    val cargando: Boolean = true,
    val registros: List<RegistroHistorial> = emptyList(),
    val total: Int = 0,
    val filtros: FiltrosHistorial = FiltrosHistorial(),
    /** Personas que aparecen en el historial, para el filtro por autor. */
    val autores: List<String> = emptyList(),
    val esAdministrador: Boolean = false
)

class HistorialViewModel(
    tareaRepository: TareaRepository,
    reporteRepository: ReporteRepository
) : ViewModel() {

    private val usuario = MutableStateFlow<Usuario?>(null)
    private val filtros = MutableStateFlow(FiltrosHistorial())

    /** Todos los registros visibles para el usuario, sin filtrar. */
    private val registrosBase = combine(
        usuario,
        tareaRepository.observarTodasLasEvidencias(),
        tareaRepository.observarValidaciones(),
        reporteRepository.observarTodos(),
        tareaRepository.observarTodas()
    ) { usuarioActual, evidencias, validaciones, reportes, tareas ->
        val esAdmin = usuarioActual?.rol == Rol.ADMINISTRADOR
        val idUsuario = usuarioActual?.id ?: -1L

        // Cada tarea creada queda en el historial desde el momento en que se
        // asigna, asi el historial se va llenando a medida que se usa la app.
        val registrosTareas = tareas
            .filter { esAdmin || it.tarea.responsableId == idUsuario }
            .map {
                RegistroHistorial(
                    id = "tarea-" + it.tarea.id,
                    tipo = TipoRegistro.TAREA,
                    titulo = it.tarea.tipo.etiqueta + ": " + it.tarea.descripcion,
                    detalle = "Asignada a " + it.responsableNombre + " en " + it.sectorNombre +
                        " para el " + Fechas.formatear(it.tarea.fechaProgramada),
                    autor = it.creadorNombre,
                    fecha = it.tarea.fechaCreacion
                )
            }

        val registrosEvidencias = evidencias
            .filter { esAdmin || it.evidencia.usuarioId == idUsuario }
            .map {
                RegistroHistorial(
                    id = "evidencia-" + it.evidencia.id,
                    tipo = TipoRegistro.EVIDENCIA,
                    titulo = "Evidencia registrada",
                    detalle = it.evidencia.comentario,
                    autor = it.usuarioNombre,
                    fecha = it.evidencia.fechaRegistro
                )
            }

        // El estudiante solo ve las revisiones de sus propias tareas.
        val registrosValidaciones = validaciones
            .filter { esAdmin || it.tareaResponsableId == idUsuario }
            .map {
            RegistroHistorial(
                id = "validacion-" + it.validacion.id,
                tipo = TipoRegistro.VALIDACION,
                titulo = it.validacion.resultado.etiqueta + ": " + it.tareaDescripcion,
                detalle = it.validacion.motivo,
                autor = it.validadorNombre,
                fecha = it.validacion.fechaValidacion
            )
        }

        val registrosReportes = reportes
            .filter { esAdmin || it.reporte.reportadoPorId == idUsuario }
            .map {
                RegistroHistorial(
                    id = "reporte-" + it.reporte.id,
                    tipo = TipoRegistro.REPORTE,
                    titulo = it.reporte.tipo.etiqueta + " en " + it.sectorNombre,
                    detalle = it.reporte.descripcion,
                    autor = it.reportanteNombre,
                    fecha = it.reporte.fechaReporte
                )
            }

        (registrosTareas + registrosEvidencias + registrosValidaciones + registrosReportes)
            .sortedByDescending { it.fecha }
    }

    val ui: StateFlow<HistorialUiState> = combine(
        registrosBase,
        filtros,
        usuario
    ) { registros, filtrosActuales, usuarioActual ->
        val hoy = Fechas.hoy()
        val filtrados = registros.filter { registro ->
            val porTipo = filtrosActuales.tipo == null || registro.tipo == filtrosActuales.tipo
            val dias = Fechas.diasEntre(registro.fecha, hoy)
            val porPeriodo = when (filtrosActuales.periodo) {
                PeriodoHistorial.TODO -> true
                PeriodoHistorial.HOY -> dias == 0
                PeriodoHistorial.SEMANA -> dias in 0..6
            }
            val porAutor = filtrosActuales.autor == null || registro.autor == filtrosActuales.autor
            porTipo && porPeriodo && porAutor
        }
        HistorialUiState(
            cargando = false,
            registros = filtrados,
            total = registros.size,
            filtros = filtrosActuales,
            autores = registros.map { it.autor }.distinct().sorted(),
            esAdministrador = usuarioActual?.rol == Rol.ADMINISTRADOR
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistorialUiState()
    )

    fun establecerUsuario(usuarioActual: Usuario) {
        usuario.value = usuarioActual
    }

    fun filtrarPorTipo(tipo: TipoRegistro?) {
        filtros.value = filtros.value.copy(tipo = tipo)
    }

    fun filtrarPorPeriodo(periodo: PeriodoHistorial) {
        filtros.value = filtros.value.copy(periodo = periodo)
    }

    fun filtrarPorAutor(autor: String?) {
        filtros.value = filtros.value.copy(autor = autor)
    }

    /** Quita todos los filtros. Los registros nunca se borran: son el historial del huerto. */
    fun limpiarFiltros() {
        filtros.value = FiltrosHistorial()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistorialViewModel(Contenedor.tareaRepository, Contenedor.reporteRepository)
            }
        }
    }
}
