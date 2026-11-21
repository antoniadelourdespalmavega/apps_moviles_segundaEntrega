package cl.duoc.huertoescolar.ui.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.domain.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import cl.duoc.huertoescolar.domain.validacion.Campos
import cl.duoc.huertoescolar.domain.validacion.revalidar
import cl.duoc.huertoescolar.domain.validacion.ValidadorLogin

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
     * Se leen de la copia local (Room), que se actualiza desde ms-usuarios.
     */
    val cuentas: StateFlow<List<Usuario>> = repositorio.observarUsuarios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(SesionUiState())
    val ui: StateFlow<SesionUiState> = _ui.asStateFlow()

    init {
        actualizarCuentas()
    }

    /** Trae las cuentas desde ms-usuarios para mostrarlas en el login. */
    fun actualizarCuentas() {
        viewModelScope.launch {
            val resultado = repositorio.actualizarCuentas()
            if (resultado is Operacion.Error && _ui.value.usuarioActual == null) {
                _ui.update { it.copy(mensaje = resultado.mensaje) }
            }
        }
    }

    fun cambiarUsuario(valor: String) {
        _ui.update { it.copy(usuario = valor, errores = it.errores.revalidar(Campos.USUARIO, ValidadorLogin.validarUsuario(valor))) }
    }

    fun cambiarClave(valor: String) {
        _ui.update { it.copy(clave = valor, errores = it.errores.revalidar(Campos.CLAVE, ValidadorLogin.validarClave(valor))) }
    }

    fun iniciarSesion() {
        val estado = _ui.value
        val errores = ValidadorLogin.validar(estado.usuario, estado.clave)
        if (errores.isNotEmpty()) {
            _ui.update { it.copy(errores = errores) }
            return
        }
        _ui.update { it.copy(cargando = true, errores = emptyMap(), mensaje = null, informacion = null) }
        viewModelScope.launch {
            // ms-usuarios revisa la contrasena y responde con el usuario o con
            // el motivo del error (clave incorrecta, servidor apagado...).
            when (val resultado = repositorio.autenticar(estado.usuario, estado.clave)) {
                is Resultado.Exito -> _ui.update {
                    it.copy(
                        cargando = false,
                        usuarioActual = resultado.dato,
                        clave = "",
                        mensaje = null
                    )
                }
                is Resultado.Error -> _ui.update {
                    it.copy(cargando = false, mensaje = resultado.mensaje)
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
