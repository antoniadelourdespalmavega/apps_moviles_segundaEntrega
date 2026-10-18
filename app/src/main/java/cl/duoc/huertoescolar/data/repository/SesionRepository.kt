package cl.duoc.huertoescolar.data.repository

import cl.duoc.huertoescolar.data.local.dao.UsuarioDao
import cl.duoc.huertoescolar.data.local.entity.Usuario
import cl.duoc.huertoescolar.domain.Cargo
import cl.duoc.huertoescolar.domain.Operacion
import cl.duoc.huertoescolar.domain.Rol
import kotlinx.coroutines.flow.Flow

/** Cuentas de usuario: inicio de sesion, registro y edicion del perfil. */
class SesionRepository(private val usuarioDao: UsuarioDao) {

    /** Crea una cuenta nueva. El usuario y el correo no se pueden repetir. */
    suspend fun registrar(
        nombre: String,
        correo: String,
        nombreUsuario: String,
        clave: String,
        cargo: Cargo,
        taller: String
    ): Operacion {
        val usuarioLimpio = nombreUsuario.trim().lowercase()
        val correoLimpio = correo.trim().lowercase()
        if (usuarioDao.contarNombreUsuario(usuarioLimpio) > 0) {
            return Operacion.Error("Ese nombre de usuario ya está en uso")
        }
        if (usuarioDao.contarCorreo(correoLimpio) > 0) {
            return Operacion.Error("Ese correo ya esta registrado")
        }
        usuarioDao.insertar(
            Usuario(
                nombreUsuario = usuarioLimpio,
                correo = correoLimpio,
                clave = clave,
                nombre = nombre.trim().replace(Regex("\\s+"), " "),
                rol = cargo.rol,
                cargo = cargo,
                taller = taller.trim()
            )
        )
        return Operacion.Exito("Cuenta creada. Ya puedes iniciar sesión")
    }

    /**
     * Actualiza nombre, correo, taller y (opcionalmente) la clave del usuario.
     * Devuelve el usuario actualizado, o un mensaje de error.
     */
    suspend fun actualizarPerfil(
        usuarioId: Long,
        nombre: String,
        correo: String,
        taller: String,
        claveActual: String,
        claveNueva: String
    ): Pair<Usuario?, Operacion> {
        val actual = usuarioDao.porId(usuarioId)
            ?: return null to Operacion.Error("La cuenta ya no existe")
        val correoLimpio = correo.trim().lowercase()
        if (usuarioDao.contarCorreo(correoLimpio, excluirId = usuarioId) > 0) {
            return null to Operacion.Error("Ese correo ya esta registrado por otra cuenta")
        }
        if (claveNueva.isNotEmpty() && claveActual != actual.clave) {
            return null to Operacion.Error("La contraseña actual no es correcta")
        }
        val actualizado = actual.copy(
            nombre = nombre.trim().replace(Regex("\\s+"), " "),
            correo = correoLimpio,
            taller = taller.trim(),
            clave = if (claveNueva.isNotEmpty()) claveNueva else actual.clave
        )
        usuarioDao.actualizar(actualizado)
        return actualizado to Operacion.Exito("Perfil actualizado")
    }

    suspend fun autenticar(nombreUsuario: String, clave: String): Usuario? =
        usuarioDao.autenticar(nombreUsuario.trim().lowercase(), clave)

    suspend fun porId(id: Long): Usuario? = usuarioDao.porId(id)

    fun observarEstudiantes(): Flow<List<Usuario>> = usuarioDao.observarPorRol(Rol.ESTUDIANTE)

    fun observarUsuarios(): Flow<List<Usuario>> = usuarioDao.observarTodos()
}
