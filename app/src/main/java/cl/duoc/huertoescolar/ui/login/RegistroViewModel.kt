package cl.duoc.huertoescolar.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.huertoescolar.Contenedor
import cl.duoc.huertoescolar.data.repository.SesionRepository
import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Rol
import cl.duoc.huertoescolar.domain.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistroUiState(
    val nombre: String = "",
    val correo: String = "",
    val usuario: String = "",
    val clave: String = "",
    val confirmar: String = "",
    val cargo: Cargo? = null,
    val taller: String = "",
    val codigoAutorizacion: String = "",
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    val guardando: Boolean = false,
    val registrado: Boolean = false
) {
    /** Docente y tallerista necesitan el codigo de autorizacion de la escuela. */
    val pideCodigo: Boolean get() = cargo?.rol == Rol.ADMINISTRADOR
}

/** Registro de cuentas nuevas desde la pantalla de inicio. */
class RegistroViewModel(private val repositorio: SesionRepository) : ViewModel() {

    private val _ui = MutableStateFlow(RegistroUiState())
    val ui: StateFlow<RegistroUiState> = _ui.asStateFlow()

    fun cambiarNombre(valor: String) = _ui.update {
        it.copy(nombre = valor, errores = it.errores - Validaciones.CAMPO_NOMBRE)
    }

    fun cambiarCorreo(valor: String) = _ui.update {
        it.copy(correo = valor, errores = it.errores - Validaciones.CAMPO_CORREO)
    }

    fun cambiarUsuario(valor: String) = _ui.update {
        it.copy(usuario = valor, errores = it.errores - Validaciones.CAMPO_USUARIO)
    }

    fun cambiarClave(valor: String) = _ui.update {
        it.copy(clave = valor, errores = it.errores - Validaciones.CAMPO_CLAVE)
    }

    fun cambiarConfirmar(valor: String) = _ui.update {
        it.copy(confirmar = valor, errores = it.errores - Validaciones.CAMPO_CONFIRMAR)
    }

    fun cambiarCargo(valor: Cargo) = _ui.update {
        it.copy(
            cargo = valor,
            errores = it.errores - Validaciones.CAMPO_CARGO - Validaciones.CAMPO_CODIGO_AUTORIZACION
        )
    }

    fun cambiarTaller(valor: String) = _ui.update {
        it.copy(taller = valor, errores = it.errores - Validaciones.CAMPO_TALLER)
    }

    fun cambiarCodigo(valor: String) = _ui.update {
        it.copy(codigoAutorizacion = valor, errores = it.errores - Validaciones.CAMPO_CODIGO_AUTORIZACION)
    }

    fun registrar() {
        val estado = _ui.value
        val errores = Validaciones.registro(
            nombre = estado.nombre,
            correo = estado.correo,
            usuario = estado.usuario,
            clave = estado.clave,
            confirmar = estado.confirmar,
            cargo = estado.cargo,
            taller = estado.taller,
            codigoAutorizacion = estado.codigoAutorizacion
        )
        if (errores.isNotEmpty()) {
            _ui.update { it.copy(errores = errores, mensaje = "Revisa los campos marcados") }
            return
        }
        _ui.update { it.copy(guardando = true, errores = emptyMap()) }
        viewModelScope.launch {
            val resultado = repositorio.registrar(
                nombre = estado.nombre,
                correo = estado.correo,
                nombreUsuario = estado.usuario,
                clave = estado.clave,
                cargo = estado.cargo ?: Cargo.ESTUDIANTE,
                taller = estado.taller
            )
            when (resultado) {
                is Operacion.Exito -> _ui.update {
                    it.copy(guardando = false, registrado = true, mensaje = resultado.mensaje)
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
            initializer { RegistroViewModel(Contenedor.sesionRepository) }
        }
    }
}
