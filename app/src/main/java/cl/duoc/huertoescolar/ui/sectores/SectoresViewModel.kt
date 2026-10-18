package cl.duoc.huertoescolar.ui.sectores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.local.entity.Cultivo
import cl.duoc.huertoescolar.data.local.entity.Sector
import cl.duoc.huertoescolar.data.local.query.SectorResumen
import cl.duoc.huertoescolar.data.repository.HuertoRepository
import cl.duoc.huertoescolar.domain.EstadoSector
import cl.duoc.huertoescolar.domain.Operacion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Listado de sectores con su resumen de cultivos y tareas. */
data class SectoresUiState(
    val cargando: Boolean = true,
    val sectores: List<SectorResumen> = emptyList(),
    val mensaje: String? = null
)

class SectoresViewModel(private val repositorio: HuertoRepository) : ViewModel() {

    private val mensaje = MutableStateFlow<String?>(null)

    val ui: StateFlow<SectoresUiState> = combine(
        repositorio.observarSectores(),
        mensaje
    ) { sectores, aviso ->
        SectoresUiState(cargando = false, sectores = sectores, mensaje = aviso)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SectoresUiState()
    )

    fun eliminarSector(id: Long) {
        viewModelScope.launch {
            when (val resultado = repositorio.eliminarSector(id)) {
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
            initializer { SectoresViewModel(Contenedor.huertoRepository) }
        }
    }
}

/** Detalle de un sector con la lista de sus cultivos. */
data class SectorDetalleUiState(
    val cargando: Boolean = true,
    val sector: Sector? = null,
    val cultivos: List<Cultivo> = emptyList(),
    val mensaje: String? = null
)

class SectorDetalleViewModel(private val repositorio: HuertoRepository) : ViewModel() {

    private val sectorId = MutableStateFlow(0L)
    private val mensaje = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ui: StateFlow<SectorDetalleUiState> = sectorId
        .flatMapLatest { id ->
            if (id <= 0L) {
                flowOf(SectorDetalleUiState(cargando = false))
            } else {
                combine(
                    repositorio.observarSector(id),
                    repositorio.observarCultivosDeSector(id),
                    mensaje
                ) { sector, cultivos, aviso ->
                    SectorDetalleUiState(
                        cargando = false,
                        sector = sector,
                        cultivos = cultivos,
                        mensaje = aviso
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SectorDetalleUiState()
        )

    fun establecerSector(id: Long) {
        sectorId.value = id
    }

    fun cambiarEstadoCultivo(cultivoId: Long, estado: cl.duoc.huertoescolar.domain.EstadoCultivo) {
        viewModelScope.launch {
            when (val resultado = repositorio.cambiarEstadoCultivo(cultivoId, estado)) {
                is Operacion.Exito -> mensaje.value = resultado.mensaje
                is Operacion.Error -> mensaje.value = resultado.mensaje
            }
        }
    }

    /** Elimina el sector; el repositorio lo impide si tiene tareas registradas. */
    fun eliminarSector(alEliminado: (String) -> Unit) {
        viewModelScope.launch {
            when (val resultado = repositorio.eliminarSector(sectorId.value)) {
                is Operacion.Exito -> alEliminado(resultado.mensaje)
                is Operacion.Error -> mensaje.value = resultado.mensaje
            }
        }
    }

    fun limpiarMensaje() {
        mensaje.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SectorDetalleViewModel(Contenedor.huertoRepository) }
        }
    }
}

/** Formulario de creacion y edicion de un sector. */
data class SectorFormUiState(
    val id: Long = 0,
    val codigo: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val ubicacion: String = "",
    val estado: EstadoSector = EstadoSector.DISPONIBLE,
    val responsableId: Long? = null,
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    val guardando: Boolean = false,
    val guardado: Boolean = false
) {
    val esEdicion: Boolean get() = id != 0L
}

class SectorFormViewModel(private val repositorio: HuertoRepository) : ViewModel() {

    private val _ui = MutableStateFlow(SectorFormUiState())
    val ui: StateFlow<SectorFormUiState> = _ui.asStateFlow()

    /**
     * Cultivos del sector que se esta editando. Es un Flow de Room: al crear o
     * editar un cultivo la lista se actualiza sola, igual que el selector de
     * cultivo de las tareas y el de los reportes de problemas.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val cultivos: StateFlow<List<Cultivo>> = _ui
        .map { it.id }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == 0L) flowOf(emptyList()) else repositorio.observarCultivosDeSector(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val responsables = Contenedor.sesionRepository.observarUsuarios()
        .map { lista -> lista.filter { it.rol == cl.duoc.huertoescolar.domain.Rol.ADMINISTRADOR } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cargar(id: Long) {
        if (id == 0L || _ui.value.id == id) return
        viewModelScope.launch {
            val sector = repositorio.sectorPorId(id) ?: return@launch
            _ui.value = SectorFormUiState(
                id = sector.id,
                codigo = sector.codigo,
                nombre = sector.nombre,
                descripcion = sector.descripcion,
                ubicacion = sector.ubicacion,
                estado = sector.estado,
                responsableId = sector.responsableId
            )
        }
    }

    fun cambiarCodigo(valor: String) = actualizar { it.copy(codigo = valor, errores = it.errores - cl.duoc.huertoescolar.domain.Validaciones.CAMPO_CODIGO) }
    fun cambiarNombre(valor: String) = actualizar { it.copy(nombre = valor, errores = it.errores - cl.duoc.huertoescolar.domain.Validaciones.CAMPO_NOMBRE) }
    fun cambiarDescripcion(valor: String) = actualizar { it.copy(descripcion = valor, errores = it.errores - cl.duoc.huertoescolar.domain.Validaciones.CAMPO_DESCRIPCION) }
    fun cambiarUbicacion(valor: String) = actualizar { it.copy(ubicacion = valor, errores = it.errores - cl.duoc.huertoescolar.domain.Validaciones.CAMPO_UBICACION) }
    fun cambiarEstado(valor: EstadoSector) = actualizar { it.copy(estado = valor) }
    fun cambiarResponsable(valor: Long?) = actualizar { it.copy(responsableId = valor) }

    private fun actualizar(bloque: (SectorFormUiState) -> SectorFormUiState) {
        _ui.update(bloque)
    }

    fun guardar() {
        val estado = _ui.value
        val errores = cl.duoc.huertoescolar.domain.Validaciones.sector(
            codigo = estado.codigo,
            nombre = estado.nombre,
            descripcion = estado.descripcion,
            ubicacion = estado.ubicacion
        )
        if (errores.isNotEmpty()) {
            _ui.update { it.copy(errores = errores, mensaje = "Revisa los campos marcados") }
            return
        }
        _ui.update { it.copy(guardando = true, errores = emptyMap()) }
        viewModelScope.launch {
            val resultado = repositorio.guardarSector(
                id = estado.id,
                codigo = estado.codigo,
                nombre = estado.nombre,
                descripcion = estado.descripcion,
                ubicacion = estado.ubicacion,
                estado = estado.estado,
                responsableId = estado.responsableId
            )
            when (resultado) {
                // Un sector nuevo queda abierto en modo edicion para agregarle
                // cultivos; una edicion vuelve al listado.
                is Operacion.Exito -> _ui.update {
                    if (estado.id == 0L) {
                        it.copy(guardando = false, id = resultado.idGenerado, mensaje = resultado.mensaje)
                    } else {
                        it.copy(guardando = false, guardado = true, mensaje = resultado.mensaje)
                    }
                }
                is Operacion.Error -> _ui.update {
                    it.copy(guardando = false, mensaje = resultado.mensaje)
                }
            }
        }
    }

    fun limpiarMensaje() {
        _ui.update { it.copy(mensaje = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SectorFormViewModel(Contenedor.huertoRepository) }
        }
    }
}
