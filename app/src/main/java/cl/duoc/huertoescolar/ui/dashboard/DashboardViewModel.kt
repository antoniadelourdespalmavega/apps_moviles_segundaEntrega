package cl.duoc.huertoescolar.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.local.query.TareaDetalle
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.ReporteRepository
import cl.duoc.huertoescolar.data.repository.TareaRepository
import cl.duoc.huertoescolar.domain.Alerta
import cl.duoc.huertoescolar.domain.Alertas
import cl.duoc.huertoescolar.domain.CategoriaTarea
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.domain.EstadoTarea
import cl.duoc.huertoescolar.domain.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Datos que muestra el panel principal, distintos segun el rol. */
data class DashboardUiState(
    val cargando: Boolean = true,
    val alertas: List<Alerta> = emptyList(),
    val totalSectores: Int = 0,
    val totalCultivos: Int = 0,
    /** Mismo conteo que usan los filtros de la pantalla de tareas. */
    val conteo: Map<CategoriaTarea, Int> = emptyMap(),
    val problemasAbiertos: Int = 0,
    val tareasDelUsuario: List<TareaDetalle> = emptyList()
)

class DashboardViewModel(
    tareaRepository: TareaRepository,
    huertoRepository: HuertoRepository,
    reporteRepository: ReporteRepository
) : ViewModel() {

    private val usuario = MutableStateFlow<Usuario?>(null)

    val ui: StateFlow<DashboardUiState> = combine(
        usuario,
        tareaRepository.observarTodas(),
        huertoRepository.observarCultivos(),
        huertoRepository.observarSectoresSimples(),
        reporteRepository.observarTodos()
    ) { usuarioActual, tareas, cultivos, sectores, reportes ->
        val hoy = Fechas.hoy()
        val esAdmin = usuarioActual?.rol == Rol.ADMINISTRADOR

        // Un estudiante solo ve informacion de sus propias tareas.
        val tareasVisibles = if (esAdmin) {
            tareas
        } else {
            tareas.filter { it.tarea.responsableId == (usuarioActual?.id ?: -1L) }
        }

        DashboardUiState(
            cargando = false,
            // Los recordatorios son para el estudiante; el docente o tallerista
            // ya ve el resumen en las tarjetas y en el listado de tareas.
            alertas = if (esAdmin) emptyList() else Alertas.calcular(tareasVisibles, cultivos, hoy),
            totalSectores = sectores.size,
            totalCultivos = cultivos.size,
            conteo = Alertas.contarPorCategoria(tareasVisibles, hoy),
            problemasAbiertos = if (esAdmin) {
                reportes.count {
                    it.reporte.estado == EstadoReporte.INFORMADO ||
                        it.reporte.estado == EstadoReporte.EN_REVISION
                }
            } else {
                reportes.count {
                    it.reporte.reportadoPorId == (usuarioActual?.id ?: -1L) &&
                        it.reporte.estado != EstadoReporte.CERRADO
                }
            },
            tareasDelUsuario = tareasVisibles
                .filter { it.tarea.estado != EstadoTarea.VALIDADA }
                .take(5)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun establecerUsuario(usuarioActual: Usuario) {
        usuario.value = usuarioActual
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    Contenedor.tareaRepository,
                    Contenedor.huertoRepository,
                    Contenedor.reporteRepository
                )
            }
        }
    }
}
