package cl.duoc.huertoescolar.ui.problemas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.local.query.ReporteDetalle
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.data.repository.ReporteRepository
import cl.duoc.huertoescolar.domain.EstadoReporte
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.TipoProblema
import cl.duoc.huertoescolar.domain.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProblemasUiState(
    val cargando: Boolean = true,
    val reportes: List<ReporteDetalle> = emptyList(),
    val esAdministrador: Boolean = false,
    val mensaje: String? = null
)

class ProblemasViewModel(private val repositorio: ReporteRepository) : ViewModel() {

    private val usuario = MutableStateFlow<Usuario?>(null)
    private val mensaje = MutableStateFlow<String?>(null)

    val ui: StateFlow<ProblemasUiState> = combine(
        usuario,
        repositorio.observarTodos(),
        mensaje
    ) { usuarioActual, reportes, aviso ->
        val esAdmin = usuarioActual?.rol == Rol.ADMINISTRADOR
        ProblemasUiState(
            cargando = false,
            // El estudiante solo ve los reportes que el mismo informo.
            reportes = if (esAdmin) {
                reportes
            } else {
                reportes.filter { it.reporte.reportadoPorId == (usuarioActual?.id ?: -1L) }
            },
            esAdministrador = esAdmin,
            mensaje = aviso
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProblemasUiState()
    )

    fun establecerUsuario(usuarioActual: Usuario) {
        usuario.value = usuarioActual
    }

    fun cambiarEstado(reporteId: Long, estado: EstadoReporte, comentario: String) {
        val usuarioActual = usuario.value ?: return
        viewModelScope.launch {
            val resultado = repositorio.actualizarEstado(
                reporteId = reporteId,
                usuarioId = usuarioActual.id,
                estado = estado,
                comentario = comentario
            )
            mensaje.value = when (resultado) {
                is Operacion.Exito -> resultado.mensaje
                is Operacion.Error -> resultado.mensaje
            }
        }
    }

    fun limpiarMensaje() {
        mensaje.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ProblemasViewModel(Contenedor.reporteRepository) }
        }
    }
}

/** Formulario para reportar un problema del huerto. */
data class ProblemaFormUiState(
    val sectorId: Long? = null,
    val cultivoId: Long? = null,
    val tipo: TipoProblema? = null,
    val descripcion: String = "",
    val rutaFoto: String? = null,
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    val guardando: Boolean = false,
    val guardado: Boolean = false
)

class ProblemaFormViewModel(
    private val reporteRepository: ReporteRepository,
    huertoRepository: HuertoRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ProblemaFormUiState())
    val ui: StateFlow<ProblemaFormUiState> = _ui.asStateFlow()

    val sectores: StateFlow<List<Sector>> = huertoRepository.observarSectoresSimples()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cultivos: StateFlow<List<Cultivo>> = huertoRepository.observarCultivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cambiarSector(valor: Long) = _ui.update {
        it.copy(sectorId = valor, cultivoId = null, errores = it.errores - Validaciones.CAMPO_SECTOR)
    }

    fun cambiarCultivo(valor: Long?) = _ui.update { it.copy(cultivoId = valor) }

    fun cambiarTipo(valor: TipoProblema) = _ui.update {
        it.copy(tipo = valor, errores = it.errores - Validaciones.CAMPO_TIPO)
    }

    fun cambiarDescripcion(valor: String) = _ui.update {
        it.copy(descripcion = valor, errores = it.errores - Validaciones.CAMPO_DESCRIPCION)
    }

    fun cambiarFoto(ruta: String?) = _ui.update { it.copy(rutaFoto = ruta) }

    fun avisar(texto: String) = _ui.update { it.copy(mensaje = texto) }

    fun registrar(usuarioId: Long) {
        val estado = _ui.value
        val errores = Validaciones.reporte(
            sectorId = estado.sectorId,
            tipo = estado.tipo,
            descripcion = estado.descripcion
        )
        if (errores.isNotEmpty()) {
            _ui.update { it.copy(errores = errores, mensaje = "Revisa los campos marcados") }
            return
        }
        _ui.update { it.copy(guardando = true, errores = emptyMap()) }
        viewModelScope.launch {
            val resultado = reporteRepository.registrar(
                sectorId = estado.sectorId ?: 0L,
                cultivoId = estado.cultivoId,
                tipo = estado.tipo ?: TipoProblema.OTRO,
                descripcion = estado.descripcion,
                usuarioId = usuarioId,
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

    fun limpiarMensaje() = _ui.update { it.copy(mensaje = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProblemaFormViewModel(Contenedor.reporteRepository, Contenedor.huertoRepository)
            }
        }
    }
}
