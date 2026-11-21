package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.data.local.dao.UsuarioDao
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.data.remote.aEntidad
import cl.duoc.huertoescolar.data.remote.api.UsuariosApi
import cl.duoc.huertoescolar.data.remote.dto.LoginRequest
import cl.duoc.huertoescolar.data.remote.dto.PerfilRequest
import cl.duoc.huertoescolar.data.remote.dto.RegistroRequest
import cl.duoc.huertoescolar.data.remote.llamarApi
import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Resultado
import cl.duoc.huertoescolar.domain.Rol
import kotlinx.coroutines.flow.Flow

/**
 * Cuentas de usuario contra ms-huerto-escolar: inicio de sesion, registro y
 * edicion del perfil. La lista de usuarios se lee de la copia local (Room).
 */
class SesionRepository(
    private val api: UsuariosApi,
    private val usuarioDao: UsuarioDao,
    private val sincronizacion: SincronizacionRepository
) {

    /** Crea una cuenta nueva en el microservicio. El usuario y el correo no se pueden repetir. */
    suspend fun registrar(
        nombre: String,
        correo: String,
        nombreUsuario: String,
        clave: String,
        cargo: Cargo,
        taller: String,
        codigoAutorizacion: String = ""
    ): Operacion {
        val solicitud = RegistroRequest(
            nombre = nombre.trim().replace(Regex("\\s+"), " "),
            correo = correo.trim().lowercase(),
            nombreUsuario = nombreUsuario.trim().lowercase(),
            clave = clave,
            cargo = cargo.name,
            taller = taller.trim(),
            codigoAutorizacion = codigoAutorizacion.trim()
        )
        return when (val resultado = llamarApi { api.registrar(solicitud) }) {
            is Resultado.Exito -> {
                sincronizacion.sincronizarUsuarios()
                Operacion.Exito("Cuenta creada. Ya puedes iniciar sesión")
            }
            is Resultado.Error -> Operacion.Error(resultado.mensaje)
        }
    }

    /**
     * Actualiza nombre, correo, taller y (opcionalmente) la clave.
     * Devuelve el usuario actualizado, o null y el mensaje de error.
     */
    suspend fun actualizarPerfil(
        usuarioId: Long,
        nombre: String,
        correo: String,
        taller: String,
        claveActual: String,
        claveNueva: String
    ): Pair<Usuario?, Operacion> {
        val solicitud = PerfilRequest(
            nombre = nombre.trim().replace(Regex("\\s+"), " "),
            correo = correo.trim().lowercase(),
            taller = taller.trim(),
            claveActual = claveActual,
            claveNueva = claveNueva
        )
        return when (val resultado = llamarApi { api.actualizarPerfil(usuarioId, solicitud) }) {
            is Resultado.Exito -> {
                val usuario = resultado.dato.aEntidad()
                sincronizacion.sincronizarUsuarios()
                usuario to Operacion.Exito("Perfil actualizado")
            }
            is Resultado.Error -> null to Operacion.Error(resultado.mensaje)
        }
    }

    /** Inicia sesion con usuario o correo. El servidor compara la contrasena. */
    suspend fun autenticar(identificador: String, clave: String): Resultado<Usuario> {
        val solicitud = LoginRequest(identificador.trim().lowercase(), clave)
        return when (val resultado = llamarApi { api.login(solicitud) }) {
            is Resultado.Exito -> Resultado.Exito(resultado.dato.aEntidad())
            is Resultado.Error -> resultado
        }
    }

    /** Actualiza la lista de cuentas (por ejemplo, para mostrarla en el login). */
    suspend fun actualizarCuentas(): Operacion = sincronizacion.sincronizarUsuarios()

    suspend fun porId(id: Long): Usuario? = usuarioDao.porId(id)

    fun observarEstudiantes(): Flow<List<Usuario>> = usuarioDao.observarPorRol(Rol.ESTUDIANTE)

    fun observarUsuarios(): Flow<List<Usuario>> = usuarioDao.observarTodos()
}
