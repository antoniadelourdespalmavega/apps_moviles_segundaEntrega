package cl.duoc.huertoescolar.ui.sincronizacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.core.Fechas
import cl.duoc.huertoescolar.data.repository.SincronizacionRepository
import cl.duoc.huertoescolar.domain.Operacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la conexion con los microservicios. */
data class SincronizacionUiState(
    val conectado: Boolean = true,
    val sincronizando: Boolean = false,
    val mensaje: String? = null,
    val ultimaSincronizacion: Long? = null
)

/**
 * Mantiene la copia local al dia: mientras la app esta abierta se sincroniza
 * con los microservicios cada pocos segundos (ver NavegacionHuerto). Asi los
 * cambios hechos desde otro telefono aparecen sin tener que recargar.
 */
class SincronizacionViewModel(
    private val repositorio: SincronizacionRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SincronizacionUiState())
    val ui: StateFlow<SincronizacionUiState> = _ui.asStateFlow()

    fun sincronizar() {
        if (_ui.value.sincronizando) return
        _ui.update { it.copy(sincronizando = true) }
        viewModelScope.launch {
            when (val resultado = repositorio.sincronizarTodo()) {
                is Operacion.Exito -> _ui.update {
                    it.copy(
                        conectado = true,
                        sincronizando = false,
                        mensaje = null,
                        ultimaSincronizacion = Fechas.ahora()
                    )
                }
                is Operacion.Error -> _ui.update {
                    it.copy(conectado = false, sincronizando = false, mensaje = resultado.mensaje)
                }
            }
        }
    }

    companion object {
        /** Cada cuanto se vuelve a sincronizar mientras la app esta en pantalla. */
        const val INTERVALO_MS = 15_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SincronizacionViewModel(Contenedor.sincronizacionRepository) }
        }
    }
}
