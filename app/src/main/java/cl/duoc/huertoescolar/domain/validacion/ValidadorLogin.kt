package cl.duoc.huertoescolar.domain.validacion

/** Formulario de inicio de sesion (LoginScreen / SesionViewModel). */
object ValidadorLogin {

    /** Se puede ingresar con el usuario o con el correo. */
    fun validarUsuario(usuario: String): String? {
        val limpio = usuario.trim()
        return when {
            limpio.isEmpty() -> "Ingresa tu usuario o correo"
            limpio.contains("@") -> ReglasComunes.correo(limpio)
            else -> null
        }
    }

    fun validarClave(clave: String): String? =
        if (clave.isEmpty()) "Ingresa tu contraseña" else null

    fun validar(usuario: String, clave: String): ErroresFormulario = errores {
        agregar(Campos.USUARIO, validarUsuario(usuario))
        agregar(Campos.CLAVE, validarClave(clave))
    }
}
