package cl.duoc.huertoescolar.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PerfilUiState(
    val usuarioId: Long = 0,
    val nombre: String = "",
    val correo: String = "",
    val taller: String = "",
    val claveActual: String = "",
    val claveNueva: String = "",
    val confirmar: String = "",
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    val guardando: Boolean = false
)

/** Edicion de los datos de la propia cuenta (nombre, correo, taller y clave). */
class PerfilViewModel(private val repositorio: SesionRepository) : ViewModel() {

    private val _ui = MutableStateFlow(PerfilUiState())
    val ui: StateFlow<PerfilUiState> = _ui.asStateFlow()

    fun cargar(usuario: Usuario) {
        if (_ui.value.usuarioId == usuario.id) return
        _ui.value = PerfilUiState(
            usuarioId = usuario.id,
            nombre = usuario.nombre,
            correo = usuario.correo,
            taller = usuario.taller
        )
    }

    fun cambiarNombre(valor: String) = _ui.update {
        it.copy(nombre = valor, errores = it.errores - Validaciones.CAMPO_NOMBRE)
    }

    fun cambiarCorreo(valor: String) = _ui.update {
        it.copy(correo = valor, errores = it.errores - Validaciones.CAMPO_CORREO)
    }

    fun cambiarTaller(valor: String) = _ui.update {
        it.copy(taller = valor, errores = it.errores - Validaciones.CAMPO_TALLER)
    }

    fun cambiarClaveActual(valor: String) = _ui.update {
        it.copy(claveActual = valor, errores = it.errores - Validaciones.CAMPO_CLAVE_ACTUAL)
    }

    fun cambiarClaveNueva(valor: String) = _ui.update {
        it.copy(claveNueva = valor, errores = it.errores - Validaciones.CAMPO_CLAVE)
    }

    fun cambiarConfirmar(valor: String) = _ui.update {
        it.copy(confirmar = valor, errores = it.errores - Validaciones.CAMPO_CONFIRMAR)
    }

    /** Guarda los cambios y entrega el usuario actualizado para refrescar la sesion. */
    fun guardar(alActualizar: (Usuario) -> Unit) {
        val estado = _ui.value
        val errores = Validaciones.perfil(
            nombre = estado.nombre,
            correo = estado.correo,
            taller = estado.taller,
            claveActual = estado.claveActual,
            claveNueva = estado.claveNueva,
            confirmar = estado.confirmar
        )
        if (errores.isNotEmpty()) {
            _ui.update { it.copy(errores = errores, mensaje = "Revisa los campos marcados") }
            return
        }
        _ui.update { it.copy(guardando = true, errores = emptyMap()) }
        viewModelScope.launch {
            val (actualizado, resultado) = repositorio.actualizarPerfil(
                usuarioId = estado.usuarioId,
                nombre = estado.nombre,
                correo = estado.correo,
                taller = estado.taller,
                claveActual = estado.claveActual,
                claveNueva = estado.claveNueva
            )
            when (resultado) {
                is Operacion.Exito -> {
                    _ui.update {
                        it.copy(
                            guardando = false,
                            mensaje = resultado.mensaje,
                            claveActual = "",
                            claveNueva = "",
                            confirmar = ""
                        )
                    }
                    actualizado?.let(alActualizar)
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
            initializer { PerfilViewModel(Contenedor.sesionRepository) }
        }
    }
}
