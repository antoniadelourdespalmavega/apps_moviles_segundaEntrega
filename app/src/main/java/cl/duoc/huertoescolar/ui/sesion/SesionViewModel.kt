package cl.duoc.huertoescolar.ui.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de inicio de sesion y de la sesion activa. */
data class SesionUiState(
    val usuario: String = "",
    val clave: String = "",
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    /** Aviso positivo (por ejemplo, "cuenta creada"). */
    val informacion: String? = null,
    val cargando: Boolean = false,
    val usuarioActual: Usuario? = null
) {
    val sesionIniciada: Boolean get() = usuarioActual != null
    val esAdministrador: Boolean get() = usuarioActual?.rol == Rol.ADMINISTRADOR
}

class SesionViewModel(private val repositorio: SesionRepository) : ViewModel() {

    /**
     * Cuentas registradas (las de prueba y las creadas con el registro).
     * Se leen de Room, asi una cuenta nueva aparece en el login al instante.
     */
    val cuentas: StateFlow<List<Usuario>> = repositorio.observarUsuarios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(SesionUiState())
    val ui: StateFlow<SesionUiState> = _ui.asStateFlow()

    fun cambiarUsuario(valor: String) {
        _ui.update { it.copy(usuario = valor, errores = it.errores - Validaciones.CAMPO_USUARIO) }
    }

    fun cambiarClave(valor: String) {
        _ui.update { it.copy(clave = valor, errores = it.errores - Validaciones.CAMPO_CLAVE) }
    }

    fun iniciarSesion() {
        val estado = _ui.value
        val errores = Validaciones.login(estado.usuario, estado.clave)
        if (errores.isNotEmpty()) {
            _ui.update { it.copy(errores = errores) }
            return
        }
        _ui.update { it.copy(cargando = true, errores = emptyMap(), mensaje = null, informacion = null) }
        viewModelScope.launch {
            val encontrado = repositorio.autenticar(estado.usuario, estado.clave)
            if (encontrado == null) {
                _ui.update {
                    it.copy(
                        cargando = false,
                        mensaje = "Usuario, correo o contraseña incorrectos"
                    )
                }
            } else {
                _ui.update {
                    it.copy(
                        cargando = false,
                        usuarioActual = encontrado,
                        clave = "",
                        mensaje = null
                    )
                }
            }
        }
    }

    /** Tras registrarse se vuelve al login con el usuario ya escrito. */
    fun cuentaCreada(nombreUsuario: String) {
        _ui.value = SesionUiState(
            usuario = nombreUsuario,
            informacion = "Cuenta creada. Ingresa tu contraseña para entrar"
        )
    }

    /** Refresca los datos de la sesion despues de editar el perfil. */
    fun actualizarUsuario(usuario: Usuario) {
        _ui.update { it.copy(usuarioActual = usuario) }
    }

    fun cerrarSesion() {
        _ui.value = SesionUiState()
    }

    fun limpiarMensaje() {
        _ui.update { it.copy(mensaje = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SesionViewModel(Contenedor.sesionRepository) }
        }
    }
}
