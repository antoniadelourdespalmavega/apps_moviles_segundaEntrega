package cl.duoc.huertoescolar.data.remote.dto

/** Usuario tal como lo entrega ms-huerto-escolar (sin contrasena). */
data class UsuarioDto(
    val id: Long,
    val nombreUsuario: String,
    val correo: String,
    val nombre: String,
    val rol: String,
    val cargo: String,
    val taller: String,
    val activo: Boolean
)

data class RegistroRequest(
    val nombre: String,
    val correo: String,
    val nombreUsuario: String,
    val clave: String,
    val cargo: String,
    val taller: String,
    val codigoAutorizacion: String
)

/** identificador: nombre de usuario o correo. */
data class LoginRequest(
    val identificador: String,
    val clave: String
)

data class PerfilRequest(
    val nombre: String,
    val correo: String,
    val taller: String,
    val claveActual: String,
    val claveNueva: String
)
